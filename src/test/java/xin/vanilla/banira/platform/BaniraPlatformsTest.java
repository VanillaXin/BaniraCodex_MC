package xin.vanilla.banira.platform;

import org.junit.Test;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.config.ConfigHolder;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class BaniraPlatformsTest {

    @Test
    public void installsAndReadsPlatformThroughFacade() {
        BaniraPlatform platform = new TestPlatform();

        BaniraPlatforms.install(platform);

        assertTrue(BaniraPlatforms.isInstalled());
        assertSame(platform, BaniraPlatforms.get());
        assertSame(platform, Banira.platform());
        assertEquals("test", Banira.platform().loaderType());
        assertNotNull(Banira.platform().configService());
    }

    private static final class TestPlatform implements BaniraPlatform {
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
            return true;
        }

        @Override
        public boolean isDedicatedServer() {
            return false;
        }

        @Override
        public boolean isDevelopment() {
            return true;
        }

        @Override
        public boolean isModLoaded(String modId) {
            return "testmod".equals(modId);
        }

        @Override
        public String modDisplayName(String modId) {
            return modId;
        }

        @Override
        public String modIdFromMainClass(Class<?> modMainClass) {
            return "testmod";
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
        public BaniraConfigService configService() {
            return NoopConfigService.INSTANCE;
        }
    }

    private enum NoopConfigService implements BaniraConfigService {
        INSTANCE;

        @Override
        public <T> void register(Class<T> configClass, String modId) {
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
