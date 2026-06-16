package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class BaniraDataPathsTest {

    @Test
    public void exposesStableConfigPath() {
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(Path.of("build", "test-config")));

        assertEquals("vanilla.xin", BaniraDataPaths.rootDirectoryName());
        assertEquals(Path.of("build", "test-config", "vanilla.xin"), BaniraDataPaths.configPath());
        assertEquals(Path.of("build", "test-world", "vanilla.xin"), BaniraDataPaths.worldDataPath());
        assertEquals(Path.of("build", "test-world", "vanilla.xin", "playerdata"), BaniraDataPaths.playerDataPath());
        assertEquals(Path.of("build", "test-world", "playerdata"), BaniraDataPaths.vanillaPlayerDataPath());
    }
}
