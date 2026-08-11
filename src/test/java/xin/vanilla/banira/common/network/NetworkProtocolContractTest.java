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
    public void neoForgePayloadRegistrarUsesExactProtocolAndOptionalMarkers() throws Exception {
        Path source = Paths.get(
                "src/main/java/xin/vanilla/banira/internal/neoforge/network/NeoForgeNetworkChannels.java"
        );
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        assertTrue(text.contains("event.registrar(protocolVersion)"));
        assertTrue(text.contains("registrar = registrar.optional()"));
        assertTrue(text.contains("playBidirectional(type, codec, handler::accept)"));
        assertFalse(text.contains("clientVersion -> true"));
        assertFalse(text.contains("serverVersion -> true"));
    }

    @Test
    public void serverPresenceReplyPrecedesChildModSynchronization() throws Exception {
        Path source = Paths.get(
                "src/main/java/xin/vanilla/banira/common/network/packet/ModLoadedToBoth.java"
        );
        String text = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);

        int reply = text.indexOf("ServerSenderAccess.sendPacket(sender, new ModLoadedToBoth(serverIds))");
        int childSync = text.indexOf("ModLoadedPresenceStore.dispatchServerSync(sender, modid)");
        assertTrue(reply >= 0);
        assertTrue(childSync >= 0);
        assertTrue("Presence reply must arrive before child mod packets", reply < childSync);
    }
}
