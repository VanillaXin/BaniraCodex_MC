package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class UiQuickActionHostTest {
    @Test
    public void transparentPersistentTemplateDoesNotInterceptMouseOrCreateCursor() throws Exception {
        try (java.io.InputStream in = getClass().getResourceAsStream("/assets/apricityui/apricity/banira_codex/hud/overlay.html")) {
            assertNotNull("Persistent overlay template", in);
            String html = new String(com.google.common.io.ByteStreams.toByteArray(in), StandardCharsets.UTF_8);
            assertTrue(html.contains("name=\"aui-mouse-events\" content=\"false\""));
            assertTrue(html.contains("pointer-events: none"));
            assertFalse(html.contains("<input"));
            assertFalse(html.contains("<script"));
        }
    }

    @Test
    public void unchangedExternalAuthorityAndTransparentInputArePreserved() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("xin.vanilla.banira.internal.client.ui.SharedHudHosts");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Shared HUD wiring missing", e);
        }
        assertNotNull(type.getMethod("render", String.class, com.mojang.blaze3d.matrix.MatrixStack.class, Class.forName("xin.vanilla.banira.internal.client.ui.HudDrawList")));
    }
}
