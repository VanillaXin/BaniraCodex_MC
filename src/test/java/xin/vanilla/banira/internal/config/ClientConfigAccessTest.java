package xin.vanilla.banira.internal.config;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueStore;
import xin.vanilla.banira.common.enums.EnumExternalInventoryButtonHost;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ClientConfigAccessTest {
    @Test
    public void externalInventoryButtonHostDefaultsToBanira() {
        assertEquals(EnumExternalInventoryButtonHost.BANIRA,
                ClientConfigAccess.root(null).externalInventoryButtonHost());
    }

    @Test
    public void externalInventoryButtonHostUsesTheConfigHolder() {
        MapStore store = new MapStore();
        store.values.put("externalInventoryButtonHost", EnumExternalInventoryButtonHost.BANIRA);
        ConfigHolder holder = ConfigHolder.create("banira_codex", "client", ConfigScope.CLIENT,
                store, Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        ClientConfig.RootView view = ClientConfigAccess.root(holder);

        assertEquals(EnumExternalInventoryButtonHost.BANIRA,
                view.externalInventoryButtonHost());

        view.externalInventoryButtonHost(EnumExternalInventoryButtonHost.FTB_LIBRARY);
        assertEquals(EnumExternalInventoryButtonHost.FTB_LIBRARY,
                store.values.get("externalInventoryButtonHost"));
    }

    private static final class MapStore implements ConfigValueStore {
        private final Map<String, Object> values = new LinkedHashMap<>();

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
            return value == null ? Object.class : value.getClass();
        }

        @Override
        public Object defaultValue(String path) {
            return EnumExternalInventoryButtonHost.ORIGINAL;
        }

        @Override
        public boolean validate(String path, Object value) {
            return value instanceof EnumExternalInventoryButtonHost;
        }

        @Override
        public void save() {
        }
    }
}
