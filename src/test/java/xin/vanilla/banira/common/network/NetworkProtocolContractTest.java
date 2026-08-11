package xin.vanilla.banira.common.network;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 自定义通道必须拒绝不同协议，只为未安装客户端保留可选登录。
 */
public class NetworkProtocolContractTest {
    @Test
    public void fabricChannelCarriesAndChecksExactProtocol() throws Exception {
        Path source = Paths.get(
                "src/main/java/xin/vanilla/banira/internal/fabric/network/FabricNetworkHandler.java"
        );
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue(text.contains("buffer.writeUtf(protocolVersion)"));
        assertTrue(text.contains("String remoteProtocol = buffer.readUtf()"));
        assertTrue(text.contains("if (!protocolVersion.equals(remoteProtocol))"));
        assertFalse(text.contains("protocolVersion = \"\";"));
    }

    @Test
    public void serverPresenceReplyPrecedesChildModSynchronization() throws Exception {
        Path source = Paths.get(
                "src/main/java/xin/vanilla/banira/common/network/packet/ModLoadedToBoth.java"
        );
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        int reply = text.indexOf("ServerSenderAccess.sendPacket(sender, new ModLoadedToBoth(serverIds))");
        int childSync = text.indexOf("ModLoadedPresence.dispatchServerSync(sender, modid)");
        assertTrue(reply >= 0);
        assertTrue(childSync >= 0);
        assertTrue("Presence reply must arrive before child mod packets", reply < childSync);
    }
}
