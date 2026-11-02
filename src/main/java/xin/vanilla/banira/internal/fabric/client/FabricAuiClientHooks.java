package xin.vanilla.banira.internal.fabric.client;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.internal.client.ui.SharedHudHosts;
import xin.vanilla.banira.internal.client.ui.UiPointerFeedback;

public final class FabricAuiClientHooks {
    private FabricAuiClientHooks() {
    }

    public static void observeButton(long window, int button, int action) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().getWindow() && client.screen instanceof UiPointerFeedback
                && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)) {
            ((UiPointerFeedback) client.screen).pointerButton(button, action == GLFW.GLFW_PRESS);
        }
    }

    public static void observeScroll(long window, double horizontal, double vertical) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().getWindow() && client.screen instanceof UiPointerFeedback) {
            ((UiPointerFeedback) client.screen).pointerScroll(vertical != 0 ? vertical : horizontal);
        }
    }

    public static void renderGame(PoseStack graphics) {
        if (Minecraft.getInstance().screen == null) SharedHudHosts.renderOthers(graphics);
    }

    public static void renderScreen(Screen screen, PoseStack graphics) {
        if (screen == Minecraft.getInstance().screen) SharedHudHosts.renderOthers(graphics);
    }
}
