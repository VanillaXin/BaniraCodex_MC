package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.BaniraPlatformsTest;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class BaniraDataPathsTest {

    @Test
    public void exposesStableBaniraDataPaths() {
        BaniraPlatforms.install(new BaniraPlatformsTest.FakePlatform());

        assertEquals("vanilla.xin", BaniraDataPaths.rootDirectoryName());
        assertEquals(Paths.get("config", "vanilla.xin"), BaniraDataPaths.configPath());
        assertEquals(Paths.get("world", "vanilla.xin"), BaniraDataPaths.worldDataPath());
        assertEquals(Paths.get("world", "vanilla.xin", "playerdata"), BaniraDataPaths.playerDataPath());
        assertEquals(Paths.get("world", "playerdata"), BaniraDataPaths.vanillaPlayerDataPath());
    }
}
