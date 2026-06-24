package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;

public class BaniraConfigsTest {

    @Test
    public void registerDelegatesToPlatformService() {
        RecordingConfigService service = new RecordingConfigService();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(service));

        BaniraConfigs.register(SampleConfig.class, "sample");

        assertSame(SampleConfig.class, service.registeredClass);
        assertEquals("sample", service.registeredModId);
    }

    @Test
    public void viewAndHandleDelegateToPlatformService() {
        RecordingConfigService service = new RecordingConfigService();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(service));

        SampleView view = BaniraConfigs.view(SampleConfig.class, SampleView.class);
        BaniraConfigHandle handle = BaniraConfigs.handle(SampleConfig.class);

        assertSame(service.sampleView, view);
        assertSame(service.handle, handle);
        assertSame(SampleConfig.class, service.viewConfigClass);
        assertSame(SampleView.class, service.viewClass);
        assertSame(SampleConfig.class, service.handleConfigClass);
    }

    private static final class SampleConfig {
    }

    private interface SampleView {
    }

    private static final class RecordingConfigService implements BaniraConfigService {
        private Class<?> registeredClass;
        private String registeredModId;
        private Class<?> viewConfigClass;
        private Class<?> viewClass;
        private Class<?> handleConfigClass;
        private final SampleView sampleView = new SampleView() {
        };
        private final BaniraConfigHandle handle = new NoopHandle();

        @Override
        public <T> void register(Class<T> configClass, String modId) {
            this.registeredClass = configClass;
            this.registeredModId = modId;
        }

        @Override
        public <T> T view(Class<?> configClass, Class<T> viewClass) {
            this.viewConfigClass = configClass;
            this.viewClass = viewClass;
            return viewClass.cast(sampleView);
        }

        @Override
        public BaniraConfigHandle handle(Class<?> configClass) {
            this.handleConfigClass = configClass;
            return handle;
        }
    }

    private static final class NoopHandle implements BaniraConfigHandle {
        @Override
        public String getModId() {
            return "sample";
        }

        @Override
        public String getConfigName() {
            return "sample";
        }

        @Override
        public void save() {
        }

        @Override
        public <T> T get(String path) {
            return null;
        }

        @Override
        public void set(String path, Object value) {
        }

        @Override
        public Set<String> valuePaths() {
            return Collections.emptySet();
        }

        @Override
        public boolean hasValue(String path) {
            return false;
        }

        @Override
        public String findValuePath(String key) {
            return null;
        }

        @Override
        public Class<?> valueClass(String path) {
            return Object.class;
        }

        @Override
        public Object defaultValue(String path) {
            return null;
        }

        @Override
        public boolean validate(String path, Object value) {
            return false;
        }

        @Override
        public boolean setIfValid(String path, Object value) {
            return false;
        }
    }
}
