package xin.vanilla.banira.internal.forge.network;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 防止服务端加载路径直接解析客户端玩家与世界类型。 */
public class ForgeDedicatedServerNetworkBoundaryTest {
    @Test
    public void serverLoadedNetworkClassesUseNeutralPlayerDescriptor() throws IOException {
        String channels = read("src/main/java/xin/vanilla/banira/internal/forge/network/ForgeNetworkChannels.java");
        String playerUtils = read("src/main/java/xin/vanilla/banira/common/util/PlayerUtils.java");
        String clientRuntime = read("src/main/java/xin/vanilla/banira/internal/client/BaniraClientRuntime.java");

        assertFalse(channels.contains("net.minecraft.client.player.LocalPlayer"));
        assertFalse(channels.contains("net.minecraft.client.Minecraft"));
        assertFalse(channels.contains("BaniraClientRuntime.localPlayer()"));
        assertTrue(channels.contains("ForgeClientNetworkAccess"));
        assertFalse(playerUtils.contains("BaniraClientRuntime."));
        assertTrue(playerUtils.contains("ClientRuntimeBridge.localPlayer()"));
        assertTrue(playerUtils.contains("ClientRuntimeBridge.levelPlayer(uuid)"));
        assertTrue(playerUtils.contains("ClientRuntimeBridge.onlinePlayerSkin(uuid)"));
        assertTrue(clientRuntime.contains("public static Player player()"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relative)), StandardCharsets.UTF_8);
    }
}
