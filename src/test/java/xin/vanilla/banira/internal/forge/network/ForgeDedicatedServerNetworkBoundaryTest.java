package xin.vanilla.banira.internal.forge.network;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 防止服务端网络发送路径再次把 LocalPlayer 写入共享类描述符。 */
public class ForgeDedicatedServerNetworkBoundaryTest {
    @Test
    public void serverLoadedNetworkClassesUseNeutralPlayerDescriptor() throws IOException {
        String channels = read("src/main/java/xin/vanilla/banira/internal/forge/network/ForgeNetworkChannels.java");
        String playerUtils = read("src/main/java/xin/vanilla/banira/common/util/PlayerUtils.java");
        String clientRuntime = read("src/main/java/xin/vanilla/banira/internal/client/BaniraClientRuntime.java");

        assertFalse(channels.contains("net.minecraft.client.player.LocalPlayer"));
        assertFalse(channels.contains("BaniraClientRuntime.localPlayer()"));
        assertFalse(playerUtils.contains("BaniraClientRuntime.localPlayer()"));
        assertTrue(playerUtils.contains("if (EnvironmentUtils.isClient())"));
        assertTrue(clientRuntime.contains("public static Player player()"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relative)), StandardCharsets.UTF_8);
    }
}
