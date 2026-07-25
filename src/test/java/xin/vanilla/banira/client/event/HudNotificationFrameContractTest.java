package xin.vanilla.banira.client.event;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** NeoForge 1.21.1 必须使用官方整帧事件绘制无界面通知。 */
public class HudNotificationFrameContractTest {
    @Test
    public void neoForgeHudPostEventOwnsScreenlessNotificationRendering() throws IOException {
        String handler = read("src/main/java/xin/vanilla/banira/client/event/BaniraClientForgeEventHandler.java");
        String config = read("src/main/resources/banira_codex.mixins.json");

        assertTrue(handler.contains("gameEventBus.addListener(RenderGuiEvent.Post.class"));
        assertTrue(handler.contains("Minecraft.getInstance().screen == null"));
        assertTrue(handler.contains("NotificationManager.get().render(event.getGuiGraphics())"));
        assertFalse(config.contains("\"injections.GuiMixin\""));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
