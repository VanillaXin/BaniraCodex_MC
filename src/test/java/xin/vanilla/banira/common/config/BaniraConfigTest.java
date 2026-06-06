package xin.vanilla.banira.common.config;

import org.junit.Test;
import xin.vanilla.banira.platform.*;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class BaniraConfigTest {

    @Test
    public void delegatesToInstalledPlatformService() {
        RecordingConfigService service = new RecordingConfigService();
        BaniraPlatforms.install(new TestPlatform(service));

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

    private record TestPlatform(BaniraConfigService configService) implements BaniraPlatform {
        @Override
        public String loaderType() {
            return "test";
        }

        @Override
        public String minecraftVersion() {
            return "0.0";
        }

        @Override
        public boolean isClient() {
            return false;
        }

        @Override
        public boolean isDedicatedServer() {
            return true;
        }

        @Override
        public boolean isDevelopment() {
            return true;
        }

        @Override
        public boolean isModLoaded(String modId) {
            return false;
        }

        @Override
        public String modDisplayName(String modId) {
            return modId;
        }

        @Override
        public String modIdFromMainClass(Class<?> modMainClass) {
            return "test";
        }

        @Override
        public Class<?> modMainClass(String modId) {
            return TestPlatform.class;
        }

        @Override
        public Path configDir() {
            return Path.of("config");
        }

        @Override
        public BaniraNetworkService networkService() {
            return NoopNetworkService.INSTANCE;
        }

        @Override
        public BaniraInputService inputService() {
            return NoopInputService.INSTANCE;
        }
    }
}
