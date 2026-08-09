package xin.vanilla.banira.common.config;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConfigHolderReloadedListenerTest {
    @Test
    public void externalReloadReportsChangesAndDropsStalePendingWrites() {
        MapStore store = new MapStore();
        store.values.put("value", 1);
        ConfigHolder holder = ConfigHolder.create("test", "test-common", ConfigScope.COMMON,
                store, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        AtomicReference<Set<String>> changed = new AtomicReference<>();
        AtomicReference<Set<String>> saved = new AtomicReference<>();
        holder.onReloaded(changed::set);
        holder.onSaved(saved::set);

        holder.set("value", 2);
        store.values.put("value", 3);
        holder.acceptExternalReload();
        holder.save();

        assertEquals(Collections.singleton("value"), changed.get());
        assertNull("reload must clear stale pending save notifications", saved.get());
        assertEquals(Integer.valueOf(3), holder.get("value"));
    }

    private static final class MapStore implements ConfigValueStore {
        private final Map<String, Object> values = new LinkedHashMap<>();
        @Override public Set<String> paths() { return values.keySet(); }
        @Override public Object get(String path) { return values.get(path); }
        @Override public void set(String path, Object value) { values.put(path, value); }
        @Override public Class<?> valueClass(String path) { return Integer.class; }
        @Override public Object defaultValue(String path) { return 1; }
        @Override public boolean validate(String path, Object value) { return true; }
        @Override public void save() { }
    }
}
