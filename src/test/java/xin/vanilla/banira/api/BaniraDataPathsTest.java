package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BaniraDataPathsTest {

    @Test
    public void exposesStableConfigPath() {
        BaniraPlatforms.install(new TestBaniraPlatform().configDir(Path.of("build", "test-config")));

        assertEquals("vanilla.xin", BaniraDataPaths.rootDirectoryName());
        assertEquals(Path.of("build", "test-config", "vanilla.xin"), BaniraDataPaths.configPath());
        assertNotNull(BaniraDataPaths.worldDataDirectory());
    }
}
