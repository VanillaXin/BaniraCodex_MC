package xin.vanilla.banira.api;

import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.BaniraServerService;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BaniraServerTest {
    @Test
    public void exposesTypedServerHandleWithoutNativeApiTypes() {
        Object server = new Object();
        BaniraPlatforms.install(new TestBaniraPlatform().serverService(service(server, true)));

        assertTrue(BaniraServer.isRunning());
        assertSame(server, BaniraServer.currentAs(Object.class));
        assertSame(server, BaniraServer.require(Object.class));
        assertNull(BaniraServer.currentAs(String.class));
    }

    @Test(expected = IllegalStateException.class)
    public void requireFailsWhenServerIsUnavailable() {
        BaniraPlatforms.install(new TestBaniraPlatform().serverService(service(null, false)));
        assertFalse(BaniraServer.isRunning());
        BaniraServer.require(Object.class);
    }

    private static BaniraServerService service(Object server, boolean running) {
        return new BaniraServerService() {
            @Override
            public Object current() {
                return server;
            }

            @Override
            public boolean isRunning() {
                return running;
            }
        };
    }
}
