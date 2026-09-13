package xin.vanilla.banira.common.config;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ConfigHolderSavedListenerTest {

    @Test
    public void reportsChangedPathsOnlyAfterSuccessfulSave() {
        MapStore store = new MapStore();
        store.values.put("concise.enabled", false);
        ConfigHolder holder = holder(store);
        AtomicReference<Set<String>> savedPaths = new AtomicReference<>();
        holder.onSaved(savedPaths::set);

        holder.set("concise.enabled", true);
        assertNull(savedPaths.get());
        holder.save();

        assertEquals(Collections.singleton("concise.enabled"), savedPaths.get());
    }

    @Test
    public void doesNotNotifyForUnchangedValuesAndSupportsUnsubscribe() {
        MapStore store = new MapStore();
        store.values.put("command.prefix", "aotake");
        ConfigHolder holder = holder(store);
        AtomicInteger calls = new AtomicInteger();
        Runnable unsubscribe = holder.onSaved(paths -> calls.incrementAndGet());

        holder.set("command.prefix", "aotake");
        holder.save();
        unsubscribe.run();
        holder.set("command.prefix", "bamboo");
        holder.save();

        assertEquals(0, calls.get());
    }

    @Test
    public void failedSaveRetainsPendingPathsForRetry() {
        MapStore store = new MapStore();
        store.values.put("concise.enabled", false);
        ConfigHolder holder = holder(store);
        AtomicReference<Set<String>> savedPaths = new AtomicReference<>();
        holder.onSaved(savedPaths::set);
        holder.set("concise.enabled", true);
        store.failSave = true;

        try {
            holder.save();
            fail("Expected save failure");
        } catch (IllegalStateException expected) {
            assertNull(savedPaths.get());
        }

        store.failSave = false;
        holder.save();
        assertEquals(Collections.singleton("concise.enabled"), savedPaths.get());
    }

    @Test
    public void reportsWriteBackToOldValueAfterReentrantReload() {
        assertReentrantLoad(false, 100, Arrays.asList("reload:321", "save:100"));
    }

    @Test
    public void acceptingReentrantReloadValueDoesNotReportLocalSave() {
        assertReentrantLoad(false, 321, Collections.singletonList("reload:321"));
    }

    @Test
    public void reportsWriteBackToOldValueAfterReentrantInitialLoad() {
        assertReentrantLoad(true, 100, Collections.singletonList("save:100"));
    }

    @Test
    public void acceptingReentrantInitialValueDoesNotReportLocalSave() {
        assertReentrantLoad(true, 321, Collections.emptyList());
    }

    private static void assertReentrantLoad(boolean initialLoad, int requested, List<String> expected) {
        MapStore store = new MapStore();
        store.values.put("chunk.limit", 100);
        ConfigHolder holder = holder(store);
        List<String> events = new ArrayList<>();
        holder.onReloaded(paths -> events.add("reload:" + holder.get("chunk.limit")));
        holder.onSaved(paths -> events.add("save:" + holder.get("chunk.limit")));
        store.beforeSet = () -> {
            store.values.put("chunk.limit", 321);
            if (initialLoad) holder.acceptInitialExternalLoad();
            else holder.acceptExternalReload();
        };

        holder.set("chunk.limit", requested);
        holder.save();
        holder.save();

        assertEquals(Integer.valueOf(requested), holder.get("chunk.limit"));
        assertEquals(expected, events);
    }

    private static ConfigHolder holder(MapStore store) {
        return ConfigHolder.create("test", "test-common.toml", ConfigScope.COMMON, store,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
    }

    private static final class MapStore implements ConfigValueStore {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private boolean failSave;
        private Runnable beforeSet;

        @Override
        public Set<String> paths() {
            return values.keySet();
        }

        @Override
        public Object get(String path) {
            return values.get(path);
        }

        @Override
        public void set(String path, Object value) {
            if (beforeSet != null) {
                Runnable callback = beforeSet;
                beforeSet = null;
                callback.run();
            }
            values.put(path, value);
        }

        @Override
        public Class<?> valueClass(String path) {
            Object value = values.get(path);
            return value != null ? value.getClass() : Object.class;
        }

        @Override
        public Object defaultValue(String path) {
            return null;
        }

        @Override
        public boolean validate(String path, Object value) {
            return true;
        }

        @Override
        public void save() {
            if (failSave) {
                throw new IllegalStateException("save failed");
            }
        }
    }
}
