package xin.vanilla.banira.client.event;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Forge 1.21.1 必须使用官方 HUD 分层系统绘制无界面通知。 */
public class HudNotificationFrameContractTest {
    @Test
    public void forgeHudLayerOwnsScreenlessNotificationRendering() throws IOException {
        String registrar = read("src/main/java/xin/vanilla/banira/internal/forge/client/ForgeNotificationLayerRegistrar.java");
        String config = read("src/main/resources/banira_codex.mixins.json");
        String hub = read("src/main/java/xin/vanilla/banira/client/event/BaniraClientEventHub.java");

        assertTrue(registrar.contains("AddGuiOverlayLayersEvent"));
        assertTrue(registrar.contains("event.getLayeredDraw().add"));
        assertTrue(registrar.contains("Minecraft.getInstance().screen == null"));
        assertTrue(registrar.contains("NotificationManager.get().render(graphics)"));
        assertFalse(config.contains("\"injections.GuiMixin\""));
        assertFalse(hub.contains("NotificationManager.get().render(event.guiGraphics())"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
