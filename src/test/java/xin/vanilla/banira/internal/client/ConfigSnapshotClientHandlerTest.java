package xin.vanilla.banira.internal.client;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueBackend;

import java.util.*;

import static org.junit.Assert.*;

public class ConfigSnapshotClientHandlerTest {

    @Test
    public void invalidSnapshotDoesNotPartiallyApply() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("first", 1);
        values.put("second", 2);
        ConfigHolder holder = holder(values,
                integerDescriptor("first", 0, 10),
                integerDescriptor("second", 0, 10));

        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("first", "5");
        snapshot.put("second", "99");

        try {
            ConfigSnapshotClientHandler.applyValidatedSnapshot(holder, snapshot);
            fail("expected invalid snapshot value");
        } catch (IllegalArgumentException expected) {
            assertEquals(Integer.valueOf(1), values.get("first"));
            assertEquals(Integer.valueOf(2), values.get("second"));
        }
    }

    @Test
    public void validSnapshotAppliesTogether() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("first", 1);
        values.put("enabled", Boolean.FALSE);
        ConfigHolder holder = holder(values,
                integerDescriptor("first", 0, 10),
                booleanDescriptor("enabled"));

        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("first", "7");
        snapshot.put("enabled", "true");

        ConfigSnapshotClientHandler.applyValidatedSnapshot(holder, snapshot);

        assertEquals(Integer.valueOf(7), values.get("first"));
        assertEquals(Boolean.TRUE, values.get("enabled"));
    }

    private static ConfigHolder holder(Map<String, Object> values, ConfigEntryDescriptor... descriptors) {
        return new ConfigHolder("test", "test-common.toml", ConfigScope.COMMON,
                new MapBackend(values), Arrays.asList(descriptors), Collections.emptyMap(), Collections.emptyMap());
    }

    private static ConfigEntryDescriptor integerDescriptor(String path, int min, int max) {
        return ConfigEntryDescriptor.builder()
                .path(path)
                .valueType(ConfigEntryDescriptor.ConfigValueType.INTEGER)
                .defaultValue(0)
                .minValue(min)
                .maxValue(max)
                .build();
    }

    private static ConfigEntryDescriptor booleanDescriptor(String path) {
        return ConfigEntryDescriptor.builder()
                .path(path)
                .valueType(ConfigEntryDescriptor.ConfigValueType.BOOLEAN)
                .defaultValue(Boolean.FALSE)
                .build();
    }

    private static final class MapBackend implements ConfigValueBackend {
        private final Map<String, Object> values;

        private MapBackend(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public Set<String> getValuePaths() {
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
        public void save() {
        }
    }
}
