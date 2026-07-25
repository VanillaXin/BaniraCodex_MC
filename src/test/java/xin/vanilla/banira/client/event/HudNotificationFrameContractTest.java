package xin.vanilla.banira.client.event;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Forge 1.19.2 必须使用官方整帧事件绘制无界面通知。 */
public class HudNotificationFrameContractTest {
    @Test
    public void forgeHudPostEventOwnsScreenlessNotificationRendering() throws IOException {
        String handler = read("src/main/java/xin/vanilla/banira/internal/forge/client/BaniraClientForgeEventHandler.java");
        String config = read("src/main/resources/banira_codex.mixins.json");
        String hub = read("src/main/java/xin/vanilla/banira/internal/client/BaniraClientEventHub.java");

        assertTrue(handler.contains("RenderGuiEvent.Post"));
        assertTrue(handler.contains("BaniraClientRuntime.currentScreen() == null"));
        assertTrue(handler.contains("BaniraClientOverlayBridge.renderHudOverlay(event.getPoseStack())"));
        assertFalse(config.contains("\"injections.GuiMixin\""));
        assertFalse(hub.contains("event.element() == HudOverlayElement.ALL"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
