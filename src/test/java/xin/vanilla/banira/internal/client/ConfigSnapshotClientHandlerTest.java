package xin.vanilla.banira.internal.client;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigListSpecHelper;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueStore;

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
        return ConfigHolder.create("test", "test-common.toml", ConfigScope.COMMON,
                new MapStore(values, descriptors), Arrays.asList(descriptors), Collections.emptyMap(), Collections.emptyMap());
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

    private static final class MapStore implements ConfigValueStore {
        private final Map<String, Object> values;
        private final Map<String, ConfigEntryDescriptor> descriptors = new LinkedHashMap<>();

        private MapStore(Map<String, Object> values, ConfigEntryDescriptor... descriptors) {
            this.values = values;
            for (ConfigEntryDescriptor descriptor : descriptors) {
                this.descriptors.put(descriptor.getPath(), descriptor);
            }
        }

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
            Object defaultValue = defaultValue(path);
            if (defaultValue != null) {
                return defaultValue.getClass();
            }
            Object value = get(path);
            return value != null ? value.getClass() : Object.class;
        }

        @Override
        public Object defaultValue(String path) {
            ConfigEntryDescriptor descriptor = descriptors.get(path);
            return descriptor != null ? descriptor.getDefaultValue() : null;
        }

        @Override
        public boolean validate(String path, Object value) {
            ConfigEntryDescriptor descriptor = descriptors.get(path);
            if (descriptor == null || value == null) {
                return false;
            }
            if (descriptor.isListType()) {
                if (!(value instanceof List)) {
                    return false;
                }
                for (Object one : (List<?>) value) {
                    if (ConfigListSpecHelper.coerceListElement(one, descriptor.getValueType(), descriptor.getEnumClass(),
                            descriptor.getMinValue(), descriptor.getMaxValue(), descriptor.getDecimalPlaces()) == null) {
                        return false;
                    }
                }
                return true;
            }
            switch (descriptor.getValueType()) {
                case INTEGER:
                case LONG:
                case DOUBLE:
                    if (!(value instanceof Number)) {
                        return false;
                    }
                    double number = ((Number) value).doubleValue();
                    return (descriptor.getMinValue() == null || number >= descriptor.getMinValue().doubleValue())
                            && (descriptor.getMaxValue() == null || number <= descriptor.getMaxValue().doubleValue());
                case BOOLEAN:
                    return value instanceof Boolean;
                case ENUM:
                    return value instanceof Enum && descriptor.getEnumClass() != null
                            && descriptor.getEnumClass().isAssignableFrom(value.getClass());
                case STRING:
                default:
                    return value instanceof String;
            }
        }

        @Override
        public void save() {
        }
    }
}
