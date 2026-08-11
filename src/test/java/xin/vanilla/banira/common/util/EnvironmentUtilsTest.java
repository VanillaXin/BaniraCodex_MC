package xin.vanilla.banira.common.util;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EnvironmentUtilsTest {

    @Test
    public void readsInstalledPlatform() {
        BaniraPlatforms.install(new TestBaniraPlatform().dedicatedServer(true));

        assertFalse(EnvironmentUtils.isClient());
        assertTrue(EnvironmentUtils.isDedicatedServer());
        assertFalse(EnvironmentUtils.isProduction());
        assertTrue(EnvironmentUtils.isDevelopment());
    }
}
