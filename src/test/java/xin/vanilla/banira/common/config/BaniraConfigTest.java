package xin.vanilla.banira.common.config;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraConfigService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BaniraConfigTest {

    @Test
    public void delegatesToInstalledPlatformService() {
        RecordingConfigService service = new RecordingConfigService();
        BaniraPlatforms.install(new TestBaniraPlatform().configService(service));

        BaniraConfig.register(SampleConfig.class, "sample");

        assertSame(SampleConfig.class, service.registeredClass);
        assertEquals("sample", service.registeredModId);
    }

    private static final class SampleConfig {
    }

    private static final class RecordingConfigService implements BaniraConfigService {
        Class<?> registeredClass;
        String registeredModId;

        @Override
        public <T> void register(Class<T> configClass, String modId) {
            this.registeredClass = configClass;
            this.registeredModId = modId;
        }

        @Override
        public <T> T get(Class<T> configClass) {
            throw new IllegalStateException("No config registered");
        }

        @Override
        public ConfigHolder holder(Class<?> configClass) {
            return null;
        }
    }
}
