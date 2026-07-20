package xin.vanilla.banira.internal.neoforge.network;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 防止 NeoForge 服务端加载路径直接解析客户端或 Forge 类型。 */
public class NeoForgeDedicatedServerNetworkBoundaryTest {
    @Test
    public void sharedPlayerAndNetworkPathsRemainClientNeutral() throws IOException {
        String playerUtils = read("src/main/java/xin/vanilla/banira/common/util/PlayerUtils.java");
        String bridge = read("src/main/java/xin/vanilla/banira/internal/common/ClientPlayerRuntimeBridge.java");
        String platform = read("src/main/java/xin/vanilla/banira/internal/neoforge/platform/NeoForgeBaniraPlatform.java");

        assertFalse(playerUtils.contains("net.minecraft.client"));
        assertTrue(playerUtils.contains("ClientPlayerRuntimeBridge.levelPlayer(uuid)"));
        assertTrue(playerUtils.contains("ClientPlayerRuntimeBridge.onlinePlayerSkin(uuid)"));
        assertTrue(bridge.contains("BaniraPlatforms.get().isClient()"));
        assertTrue(bridge.contains("Class.forName(RUNTIME_CLASS)"));
        assertFalse(platform.contains("LocalPlayer"));
        assertFalse(platform.contains("net.minecraftforge"));
    }

    private static String read(String relative) throws IOException {
        return new String(Files.readAllBytes(Paths.get(relative)), StandardCharsets.UTF_8);
    }
}
