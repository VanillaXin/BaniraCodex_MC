package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConfigAdapterConcurrencyTest {
    private static class Marker {}
    public interface CountView { int count(); }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> cache(String name) throws Exception {
        Field field = ForgeConfigAdapter.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<K, V>) field.get(null);
    }

    private static Fixture fixture(int id) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        Map<String, ForgeConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
        values.put("count", builder.define("count", 0));
        ForgeConfigSpec spec = builder.build();
        CommentedConfig data = CommentedConfig.inMemory();
        data.set("count", 0);
        spec.setConfig(data);
        ForgeConfigValueStore store = new ForgeConfigValueStore(spec, values);
        ConfigHolder holder = ConfigHolder.create("adapter_concurrency", "cache_" + id, ConfigScope.COMMON,
                store, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        store.setHolder(holder);
        return new Fixture(holder, store);
    }

    private static ModConfig nativeIdentity() throws Exception {
        // Cache identity does not require attaching a synthetic mod to ConfigTracker.
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (ModConfig) unsafeType.getMethod("allocateInstance", Class.class)
                .invoke(field.get(null), ModConfig.class);
    }

    private static <K, V> void assertGuardedPublication(Map<K, V> cache, K key, V value) throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            entered.countDown();
            cache.put(key, value);
            done.countDown();
        }, "adapter-cache-publication");
        try {
            synchronized (cache) {
                writer.start();
                assertTrue(entered.await(5, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (done.getCount() != 0 && writer.getState() != Thread.State.BLOCKED
                        && System.nanoTime() < deadline) Thread.yield();
                assertEquals("Cache iteration guard must also exclude concurrent publication",
                        Thread.State.BLOCKED, writer.getState());
                assertEquals(1, done.getCount());
            }
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertSame(value, cache.get(key));
        } finally {
            writer.join(5000);
            cache.remove(key);
        }
    }

    @Test public void classCacheCoordinatesPublicationWithOrderedSnapshots() throws Exception {
        assertGuardedPublication(cache("HOLDER_MAP"), Marker.class, fixture(1).holder);
    }

    @Test public void nativeIdentityCacheCoordinatesPublicationWithReaders() throws Exception {
        assertGuardedPublication(cache("HOLDER_BY_CONFIG"), nativeIdentity(), fixture(2).holder);
    }

    @Test public void replacementsKeepClassInsertionOrderAndNativeIdentity() throws Exception {
        Map<Class<?>, ConfigHolder> classes = cache("HOLDER_MAP");
        Map<ModConfig, ConfigHolder> identities = cache("HOLDER_BY_CONFIG");
        Class<?> a = Array.newInstance(Marker.class, new int[1]).getClass();
        Class<?> b = Array.newInstance(Marker.class, new int[2]).getClass();
        Fixture first = fixture(3), replacement = fixture(4);
        ModConfig left = nativeIdentity(), right = nativeIdentity();
        try {
            classes.put(a, first.holder);
            classes.put(b, first.holder);
            classes.put(a, replacement.holder);
            List<Class<?>> order;
            synchronized (classes) { order = new ArrayList<>(classes.keySet()); }
            assertTrue(order.indexOf(a) < order.indexOf(b));
            assertSame(replacement.holder, ForgeConfigAdapter.getHolder(a));
            identities.put(left, first.holder);
            identities.put(right, replacement.holder);
            assertSame(first.holder, identities.get(left));
            assertSame(replacement.holder, identities.get(right));
            identities.remove(left);
            assertSame(replacement.holder, identities.get(right));
        } finally {
            classes.remove(a); classes.remove(b);
            identities.remove(left); identities.remove(right);
        }
    }

    @Test public void concurrentRealCachePublicationAllowsImmediateHandleViewAndSavedListener() throws Exception {
        Map<Class<?>, ConfigHolder> classes = cache("HOLDER_MAP");
        Map<ModConfig, ConfigHolder> identities = cache("HOLDER_BY_CONFIG");
        List<Class<?>> keys = new ArrayList<>();
        List<ModConfig> nativeKeys = new ArrayList<>();
        List<Fixture> fixtures = new ArrayList<>();
        for (int i = 0; i < 128; i++) {
            keys.add(Array.newInstance(Marker.class, new int[i + 3]).getClass());
            nativeKeys.add(nativeIdentity());
            fixtures.add(fixture(i + 10));
        }
        ExecutorService workers = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger saved = new AtomicInteger();
        CountDownLatch writersDone = new CountDownLatch(keys.size());
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int reader = 0; reader < 2; reader++) {
                futures.add(workers.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    while (writersDone.getCount() > 0 && !Thread.currentThread().isInterrupted()) {
                        synchronized (classes) {
                            for (Map.Entry<Class<?>, ConfigHolder> entry : classes.entrySet()) {
                                assertNotNull(entry.getKey()); assertNotNull(entry.getValue());
                            }
                        }
                        Thread.yield();
                    }
                    return null;
                }));
            }
            for (int i = 0; i < keys.size(); i++) {
                final int index = i;
                futures.add(workers.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    try {
                        Class<?> key = keys.get(index);
                        Fixture f = fixtures.get(index);
                        classes.put(key, f.holder);
                        identities.put(nativeKeys.get(index), f.holder);
                        ConfigHolder immediate = (ConfigHolder) ForgeBaniraConfigService.INSTANCE.handle(key);
                        assertSame(f.holder, immediate);
                        Runnable remove = immediate.onSaved(changed -> {
                            assertFalse(Thread.holdsLock(classes));
                            assertFalse(Thread.holdsLock(identities));
                            assertSame(f.holder, ForgeConfigAdapter.getHolder(key));
                            saved.incrementAndGet();
                        });
                        try {
                            immediate.set("count", index + 1);
                            assertEquals(index + 1, ForgeConfigAdapter.view(key, CountView.class).count());
                            immediate.save();
                        } finally { remove.run(); }
                        assertSame(f.holder, identities.get(nativeKeys.get(index)));
                        return null;
                    } finally { writersDone.countDown(); }
                }));
            }
            start.countDown();
            for (Future<?> future : futures) future.get(15, TimeUnit.SECONDS);
            assertEquals(128, saved.get());
            for (int i = 0; i < keys.size(); i++) {
                assertSame(fixtures.get(i).holder, ForgeConfigAdapter.getHolder(keys.get(i)));
                assertSame(fixtures.get(i).holder, identities.get(nativeKeys.get(i)));
            }
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
            for (Class<?> key : keys) classes.remove(key);
            for (ModConfig key : nativeKeys) identities.remove(key);
        }
    }

    private static final class Fixture {
        final ConfigHolder holder;
        final ForgeConfigValueStore store;
        Fixture(ConfigHolder holder, ForgeConfigValueStore store) {
            this.holder = holder; this.store = store;
        }
    }
}
