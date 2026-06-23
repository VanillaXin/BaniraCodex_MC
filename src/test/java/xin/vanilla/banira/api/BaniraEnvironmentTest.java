package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.*;

public class BaniraEnvironmentTest {

    @Test
    public void readsEnvironmentThroughPlatform() {
        BaniraPlatforms.install(new TestBaniraPlatform()
                .loaderType("fabric")
                .minecraftVersion("1.19.2")
                .client(true)
                .development(false)
                .mod("example", TestBaniraPlatform.class));

        assertEquals("fabric", BaniraEnvironment.loaderType());
        assertEquals("1.19.2", BaniraEnvironment.minecraftVersion());
        assertTrue(BaniraEnvironment.isClient());
        assertFalse(BaniraEnvironment.isDedicatedServer());
        assertFalse(BaniraEnvironment.isDevelopment());
        assertTrue(BaniraEnvironment.isProduction());
        assertTrue(BaniraEnvironment.isModLoaded("example"));
        assertFalse(BaniraEnvironment.isModLoaded("missing"));
    }
}
