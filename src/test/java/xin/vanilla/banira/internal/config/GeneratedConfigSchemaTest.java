package xin.vanilla.banira.internal.config;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class GeneratedConfigSchemaTest {
    @Test
    public void generatedCommonSchemaMatchesRealRegistration() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        bind(CommonConfig.class, fixture.holder);
        assertEquals(fixture.defaults, read(CommonConfigView.get()));
    }

    @Test
    public void generatedClientSchemaMatchesRealRegistration() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        bind(ClientConfig.class, fixture.holder);
        assertEquals(fixture.defaults, read(ClientConfigView.get()));
    }

    static void bind(Class<?> configClass, ConfigHolder holder) {
        BaniraPlatforms.install(new TestBaniraPlatform().configService(new BaniraConfigService() {
            public <T> void register(Class<T> type, String modId) { throw new UnsupportedOperationException(); }
            public <T> T view(Class<?> type, Class<T> view) { throw new UnsupportedOperationException(); }
            public BaniraConfigHandle handle(Class<?> type) { return type == configClass ? holder : null; }
        }));
    }

    static Map<String, Object> read(Object view) throws Exception {
        Map<String, Object> values = new TreeMap<>();
        collect(view, "", values);
        return values;
    }

    private static void collect(Object view, String prefix, Map<String, Object> values) throws Exception {
        for (Method method : view.getClass().getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0 || method.getName().equals("handle")) continue;
            Object value = method.invoke(view);
            String path = prefix + method.getName();
            if (method.getReturnType().getEnclosingClass() == view.getClass()) {
                collect(value, path + ".", values);
            } else {
                assertNotNull(view.getClass().getMethod(method.getName(), method.getReturnType()));
                values.put(path, value);
            }
        }
    }
}
