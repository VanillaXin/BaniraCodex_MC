package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class UiQuickActionHostTest {
    @Test public void transparentPersistentTemplateDoesNotInterceptMouseOrCreateCursor() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/hud/overlay.html")) {
            assertNotNull("Persistent overlay template", in);
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(html.contains("name=\"aui-mouse-events\" content=\"false\""));
            assertTrue(html.contains("pointer-events: none")); assertFalse(html.contains("<input")); assertFalse(html.contains("<script"));
        }
    }
    @Test public void unchangedExternalAuthorityAndTransparentInputArePreserved() throws Exception {
        Class<?> type;
        try { type = Class.forName("xin.vanilla.banira.internal.client.ui.SharedHudHosts"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Shared HUD wiring missing", e); }
        assertNotNull(type.getMethod("render", String.class, net.minecraft.client.gui.GuiGraphics.class, Class.forName("xin.vanilla.banira.internal.client.ui.HudDrawList")));
    }
}
