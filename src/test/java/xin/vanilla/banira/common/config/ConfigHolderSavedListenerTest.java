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

    private static ConfigHolder holder(MapStore store) {
        return ConfigHolder.create("test", "test-common.toml", ConfigScope.COMMON, store,
                Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
    }

    private static final class MapStore implements ConfigValueStore {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private boolean failSave;

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
