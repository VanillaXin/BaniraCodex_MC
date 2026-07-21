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
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/banira/internal/fabric/network/FabricNetworkChannels.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("ClientRuntimeBridge.localPlayer()"));
        assertFalse(source.contains("BaniraClientRuntime"));
        assertFalse(source.contains("LocalPlayer"));
    }
}
