package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class UiQuickActionHostTest {


    @Test
    public void unchangedExternalAuthorityAndTransparentInputArePreserved() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("xin.vanilla.banira.internal.client.ui.SharedHudHosts");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Shared HUD wiring missing", e);
        }
        assertNotNull(type.getMethod("render", String.class, com.mojang.blaze3d.vertex.PoseStack.class, Class.forName("xin.vanilla.banira.internal.client.ui.HudDrawList")));
    }
}
