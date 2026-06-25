package xin.vanilla.banira.common.config;

import org.junit.Test;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BaniraConfigsTest {

    @Test
    public void delegatesToInstalledPlatformService() {
        RecordingConfigService service = new RecordingConfigService();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(service));

        BaniraConfigs.register(SampleConfig.class, "sample");

        assertSame(SampleConfig.class, service.registeredClass);
        assertEquals("sample", service.registeredModId);
    }

    @Test
    public void viewDelegatesToInstalledPlatformService() {
        RecordingConfigService service = new RecordingConfigService();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(service));

        SampleView view = BaniraConfigs.view(SampleConfig.class, SampleView.class);

        assertSame(service.sampleView, view);
        assertSame(SampleConfig.class, service.viewConfigClass);
        assertSame(SampleView.class, service.viewClass);
    }

    private static final class SampleConfig {
    }

    private interface SampleView {
    }

    private static final class RecordingConfigService implements BaniraConfigService {
        Class<?> registeredClass;
        String registeredModId;
        Class<?> viewConfigClass;
        Class<?> viewClass;
        final SampleView sampleView = new SampleView() {
        };

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
            return null;
        }
    }
}
