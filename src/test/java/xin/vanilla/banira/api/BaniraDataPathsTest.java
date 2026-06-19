package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPathService;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class BaniraDataPathsTest {

    @Test
    public void exposesStableBaniraDataPaths() {
        BaniraPlatforms.install(new TestBaniraPlatform().pathService(new BaniraPathService() {
            @Override
            public String rootDirectoryName() {
                return "vanilla.xin";
            }

            @Override
            public Path configPath() {
                return Paths.get("config", rootDirectoryName());
            }

            @Override
            public Path worldDataPath() {
                return Paths.get("world", rootDirectoryName());
            }

            @Override
            public Path playerDataPath() {
                return worldDataPath().resolve("playerdata");
            }

            @Override
            public Path vanillaPlayerDataPath() {
                return Paths.get("world", "playerdata");
            }
        }));

        assertEquals("vanilla.xin", BaniraDataPaths.rootDirectoryName());
        assertEquals(Paths.get("config", "vanilla.xin"), BaniraDataPaths.configPath());
        assertEquals(Paths.get("world", "vanilla.xin"), BaniraDataPaths.worldDataPath());
        assertEquals(Paths.get("world", "vanilla.xin", "playerdata"), BaniraDataPaths.playerDataPath());
        assertEquals(Paths.get("world", "playerdata"), BaniraDataPaths.vanillaPlayerDataPath());
    }
}
