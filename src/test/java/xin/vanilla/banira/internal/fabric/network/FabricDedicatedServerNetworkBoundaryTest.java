package xin.vanilla.banira.internal.fabric.network;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 防止服务端可达的 Fabric 网络发送类解析客户端玩家类型。 */
public class FabricDedicatedServerNetworkBoundaryTest {
    @Test
    public void sharedNetworkChannelUsesTheNeutralClientRuntimeBridge() throws Exception {
        String channels = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/fabric/network/FabricNetworkChannels.java")),
                StandardCharsets.UTF_8);
        String handler = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/fabric/network/FabricNetworkHandler.java")),
                StandardCharsets.UTF_8);

        assertTrue(channels.contains("ClientRuntimeBridge.localPlayer()"));
        assertFalse(channels.contains("BaniraClientRuntime"));
        assertFalse(channels.contains("LocalPlayer"));
        assertTrue(handler.contains("ClientRuntimeBridge.level()"));
        assertFalse(handler.contains("BaniraClientRuntime"));
    }
}
