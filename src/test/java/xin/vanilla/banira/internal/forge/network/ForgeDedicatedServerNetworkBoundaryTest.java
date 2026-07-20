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
    public void sharedPlayerAndNetworkPathsRemainClientNeutral() throws IOException {
        String playerUtils = read("src/main/java/xin/vanilla/banira/common/util/PlayerUtils.java");
        String bridge = read("src/main/java/xin/vanilla/banira/internal/common/ClientPlayerRuntimeBridge.java");
        String platform = read("src/main/java/xin/vanilla/banira/internal/forge/platform/ForgeBaniraPlatform.java");

        assertFalse(playerUtils.contains("net.minecraft.client"));
        assertTrue(playerUtils.contains("ClientPlayerRuntimeBridge.levelPlayer(uuid)"));
        assertTrue(playerUtils.contains("ClientPlayerRuntimeBridge.onlinePlayerSkin(uuid)"));
        assertTrue(bridge.contains("BaniraPlatforms.get().isClient()"));
        assertTrue(bridge.contains("Class.forName(RUNTIME_CLASS)"));
        assertFalse(platform.contains("LocalPlayer"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relative)), StandardCharsets.UTF_8);
    }
}
