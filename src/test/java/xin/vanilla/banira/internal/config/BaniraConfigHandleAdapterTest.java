package xin.vanilla.banira.internal.config;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueBackend;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class BaniraConfigHandleAdapterTest {
    @Test
    public void adapterValidatesAndWritesThroughHolder() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("base.count", 3);
        ConfigHolder holder = new ConfigHolder("demo", "common", ConfigScope.COMMON, new MapBackend(values),
                Collections.singletonList(ConfigEntryDescriptor.builder()
                        .path("base.count")
                        .valueType(ConfigEntryDescriptor.ConfigValueType.INTEGER)
                        .defaultValue(1)
                        .minValue(0)
                        .maxValue(5)
                        .build()),
                Collections.emptyMap(),
                Collections.emptyMap());

        BaniraConfigHandle handle = new BaniraConfigHandleAdapter(holder);

        assertEquals("base.count", handle.findValuePath("count"));
        assertTrue(handle.validate("base.count", 5));
        assertFalse(handle.validate("base.count", 6));
        assertTrue(handle.setIfValid("base.count", 4));
        assertEquals(Integer.valueOf(4), handle.get("base.count"));
        assertFalse(handle.setIfValid("base.count", "bad"));
        assertEquals(Integer.valueOf(4), handle.get("base.count"));
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
