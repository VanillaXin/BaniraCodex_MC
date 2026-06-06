package xin.vanilla.banira.common.util;

import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.*;

import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnvironmentUtilsTest {

    @Test
    public void readsInstalledPlatform() {
        BaniraPlatforms.install(new ServerLikePlatform());

        assertFalse(EnvironmentUtils.isClient());
        assertTrue(EnvironmentUtils.isDedicatedServer());
        assertFalse(EnvironmentUtils.isProduction());
        assertTrue(EnvironmentUtils.isDevelopment());
    }

    private static final class ServerLikePlatform implements BaniraPlatform {
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
            return ServerLikePlatform.class;
        }

        @Override
        public Path configDir() {
            return Path.of("config");
        }

        @Override
        public BaniraConfigService configService() {
            return NoopConfigService.INSTANCE;
        }

        @Override
        public BaniraNetworkService networkService() {
            return NoopNetworkService.INSTANCE;
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
