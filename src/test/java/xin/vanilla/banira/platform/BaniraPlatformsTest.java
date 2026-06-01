package xin.vanilla.banira.platform;

import org.junit.Test;
import xin.vanilla.banira.platform.config.BaniraConfigService;
import xin.vanilla.banira.platform.event.BaniraLifecycle;
import xin.vanilla.banira.platform.network.BaniraNetworkService;
import xin.vanilla.banira.platform.registry.BaniraRegistryService;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class BaniraPlatformsTest {
    @Test
    public void installExposesActivePlatform() {
        BaniraPlatform platform = new FakePlatform();

        BaniraPlatforms.install(platform);

        assertTrue(BaniraPlatforms.isInstalled());
        assertSame(platform, BaniraPlatforms.get());
        assertEquals("test", BaniraPlatforms.get().loaderType());
    }

    private static final class FakePlatform implements BaniraPlatform {
        @Override
        public String loaderType() {
            return "test";
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
        public Path configDir() {
            return Paths.get("config");
        }

        @Override
        public BaniraLifecycle lifecycle() {
            return null;
        }

        @Override
        public BaniraConfigService config() {
            return null;
        }

        @Override
        public BaniraNetworkService network() {
            return null;
        }

        @Override
        public BaniraRegistryService registry() {
            return null;
        }
    }
}
