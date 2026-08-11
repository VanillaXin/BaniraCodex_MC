package xin.vanilla.banira.client.event;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** NeoForge 1.21.1 必须通过官方分层 HUD 注册绘制无界面通知。 */
public class HudNotificationFrameContractTest {
    @Test
    public void neoForgeHudLayerOwnsScreenlessNotificationRendering() throws IOException {
        String registrar = read("src/main/java/xin/vanilla/banira/internal/neoforge/client/NeoForgeNotificationLayerRegistrar.java");
        String handler = read("src/main/java/xin/vanilla/banira/internal/neoforge/client/BaniraClientNeoForgeEventHandler.java");
        String config = read("src/main/resources/banira_codex.mixins.json");

        assertTrue(registrar.contains("RegisterGuiLayersEvent"));
        assertTrue(registrar.contains("registerAboveAll"));
        assertTrue(registrar.contains("Minecraft.getInstance().screen == null"));
        assertTrue(registrar.contains("NotificationManager.get().render(graphics)"));
        assertTrue(config.contains("\"injections.GuiHudLayerMixin\""));
        assertFalse(registrar.contains("RenderGuiEvent"));
        assertTrue(handler.contains("onGuiScreen(ScreenEvent.Render.Pre event)"));
        assertFalse(handler.contains("onGuiScreen(ScreenEvent event)"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
