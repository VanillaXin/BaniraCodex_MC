package xin.vanilla.banira.platform;

import org.junit.Test;
import xin.vanilla.banira.api.Banira;

import static org.junit.Assert.*;

public class BaniraPlatformsTest {

    @Test
    public void installsAndReadsPlatformThroughFacade() {
        BaniraPlatform platform = new TestBaniraPlatform()
                .client(true)
                .mod("testmod", TestBaniraPlatform.class);

        BaniraPlatforms.install(platform);

        assertTrue(BaniraPlatforms.isInstalled());
        assertSame(platform, BaniraPlatforms.get());
        assertSame(platform, Banira.platform());
        assertEquals("test", Banira.platform().loaderType());
        assertNotNull(Banira.platform().configService());
    }
}
