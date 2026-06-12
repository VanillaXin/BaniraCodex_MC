package xin.vanilla.banira.platform;

import org.junit.Test;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.NetworkPacket;

import static org.junit.Assert.assertTrue;

public class BaniraNetworkApiTest {
    @Test
    public void legacyPacketTypesExposeRootNetworkMarker() {
        assertTrue(BaniraNetworkPacket.class.isAssignableFrom(INetworkPacket.class));
        assertTrue(BaniraNetworkPacket.class.isAssignableFrom(NetworkPacket.class));
    }
}
