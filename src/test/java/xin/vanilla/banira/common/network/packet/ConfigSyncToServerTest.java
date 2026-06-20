package xin.vanilla.banira.common.network.packet;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueBackend;

import java.util.*;

import static org.junit.Assert.*;

public class ConfigSyncToServerTest {

    @Test
    public void appliesChangesOnlyAfterAllValuesValidate() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("first", 1);
        values.put("second", 2);
        ConfigHolder holder = holder(values,
                integerDescriptor("first", 0, 10),
                integerDescriptor("second", 0, 10));

        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("first", "5");
        changes.put("second", "99");

        try {
            ConfigSyncToServer.applyValidatedChanges(holder, changes);
            fail("expected invalid config value");
        } catch (IllegalArgumentException expected) {
            assertEquals(Integer.valueOf(1), values.get("first"));
            assertEquals(Integer.valueOf(2), values.get("second"));
        }
    }

    @Test
    public void invalidBooleanIsRejectedInsteadOfParsedAsFalse() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("flag", Boolean.TRUE);
        ConfigHolder holder = holder(values, booleanDescriptor("flag"));

        assertEquals("maybe", ConfigSyncToServer.decodeNetworkValue(holder, "flag", "maybe"));

        try {
            ConfigSyncToServer.applyValidatedChanges(holder, Collections.singletonMap("flag", "maybe"));
            fail("expected invalid boolean value");
        } catch (IllegalArgumentException expected) {
            assertEquals(Boolean.TRUE, values.get("flag"));
        }
    }

    @Test
    public void validChangesAreAppliedTogether() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("first", 1);
        values.put("flag", Boolean.FALSE);
        ConfigHolder holder = holder(values,
                integerDescriptor("first", 0, 10),
                booleanDescriptor("flag"));

        Map<String, String> changes = new LinkedHashMap<>();
        changes.put("first", "6");
        changes.put("flag", "true");

        ConfigSyncToServer.applyValidatedChanges(holder, changes);

        assertEquals(Integer.valueOf(6), values.get("first"));
        assertEquals(Boolean.TRUE, values.get("flag"));
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
