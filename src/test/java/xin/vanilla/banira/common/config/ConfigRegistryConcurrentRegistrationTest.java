package xin.vanilla.banira.common.config;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConfigRegistryConcurrentRegistrationTest {
    private final String prefix = "parallel-" + UUID.randomUUID() + "-";

    @After
    public void cleanupOwnedEntries() throws Exception {
        Field field = ConfigRegistry.class.getDeclaredField("HOLDERS");
        field.setAccessible(true);
        synchronized (ConfigRegistry.class) {
            @SuppressWarnings("unchecked") Map<String, ConfigHolder> holders =
                    (Map<String, ConfigHolder>) field.get(null);
            holders.keySet().removeIf(key -> key.startsWith(prefix));
        }
    }

    @Test
    public void concurrentRegistrationPublishesEveryHolderAndSupportsSnapshots() throws Exception {
        int writers = 8, perWriter = 512;
        ExecutorService pool = Executors.newFixedThreadPool(writers + 2);
        CountDownLatch ready = new CountDownLatch(writers + 2), start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(writers);
        AtomicInteger snapshots = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < writers; i++) {
                final int writer = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    await(start);
                    try {
                        for (int n = 0; n < perWriter; n++) {
                            ConfigHolder holder = holder(prefix + writer + "-" + n);
                            ConfigRegistry.registerHolder(holder);
                            assertSame("A publisher must immediately find its own registered holder",
                                    holder, ConfigRegistry.get(holder.getConfigName()));
                        }
                    } finally {
                        done.countDown();
                    }
                }));
            }
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    await(start);
                    do {
                        Map<String, ConfigHolder> snapshot = ConfigRegistry.getAll();
                        for (Map.Entry<String, ConfigHolder> entry : snapshot.entrySet()) {
                            assertEquals(entry.getKey(), entry.getValue().getConfigName());
                        }
                        snapshots.incrementAndGet();
                        Thread.yield();
                    } while (done.getCount() != 0);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(20, TimeUnit.SECONDS);
            long count = ConfigRegistry.getAll().keySet().stream().filter(key -> key.startsWith(prefix)).count();
            assertEquals(writers * perWriter, count);
            assertTrue(snapshots.get() > 0);
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue("All owned publisher threads must finish", pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    public void snapshotsRemainDetachedAndKeepInsertionOrder() {
        ConfigHolder first = holder(prefix + "first"), second = holder(prefix + "second");
        ConfigRegistry.registerHolder(first);
        ConfigRegistry.registerHolder(second);
        Map<String, ConfigHolder> snapshot = ConfigRegistry.getAll();
        ConfigHolder later = holder(prefix + "later");
        ConfigRegistry.registerHolder(later);
        assertFalse(snapshot.containsKey(later.getConfigName()));
        List<String> owned = new ArrayList<>();
        for (String key : snapshot.keySet()) if (key.startsWith(prefix)) owned.add(key);
        assertEquals(Arrays.asList(first.getConfigName(), second.getConfigName()), owned);
        snapshot.remove(first.getConfigName());
        assertSame(first, ConfigRegistry.get(first.getConfigName()));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Publisher start deadline");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static ConfigHolder holder(String name) {
        return ConfigHolder.create("parallel-test", name, ConfigScope.COMMON, new EmptyStore(),
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
    }

    private static final class EmptyStore implements ConfigValueStore {
        @Override public Set<String> paths() { return Collections.emptySet(); }
        @Override public Object get(String path) { return null; }
        @Override public void set(String path, Object value) { throw new UnsupportedOperationException(); }
        @Override public Class<?> valueClass(String path) { return Object.class; }
        @Override public Object defaultValue(String path) { return null; }
        @Override public boolean validate(String path, Object value) { return false; }
        @Override public void save() {}
    }
}
