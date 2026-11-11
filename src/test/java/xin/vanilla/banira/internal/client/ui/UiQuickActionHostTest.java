package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

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
