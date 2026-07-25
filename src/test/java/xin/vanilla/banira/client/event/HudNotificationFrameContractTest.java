package xin.vanilla.banira.client.event;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Forge 1.20.1 必须在真实 HUD 帧末绘制通知，不能依赖玩家列表实际出现。 */
public class HudNotificationFrameContractTest {
    @Test
    public void forgeHudPostEventOwnsScreenlessNotificationRendering() throws IOException {
        String handler = read("src/main/java/xin/vanilla/banira/client/event/BaniraClientForgeEventHandler.java");
        String config = read("src/main/resources/banira_codex.mixins.json");
        String hub = read("src/main/java/xin/vanilla/banira/client/event/BaniraClientEventHub.java");

        assertTrue(handler.contains("RenderGuiEvent.Post"));
        assertTrue(handler.contains("Minecraft.getInstance().screen == null"));
        assertTrue(handler.contains("NotificationManager.get().render(event.getGuiGraphics())"));
        assertFalse(config.contains("\"injections.GuiMixin\""));
        assertFalse(hub.contains("NotificationManager.get().render(event.guiGraphics())"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
