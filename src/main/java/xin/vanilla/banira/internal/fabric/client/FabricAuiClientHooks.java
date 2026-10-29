package xin.vanilla.banira.internal.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.internal.client.ui.SharedHudHosts;
import xin.vanilla.banira.internal.client.ui.UiPointerFeedback;

public final class FabricAuiClientHooks {
    private FabricAuiClientHooks() {
    }

    public static void observeButton(long window, int button, int action) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().getWindow() && client.screen instanceof UiPointerFeedback pointer
                && (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_RELEASE)) {
            pointer.pointerButton(button, action == GLFW.GLFW_PRESS);
        }
    }

    public static void observeScroll(long window, double horizontal, double vertical) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().getWindow() && client.screen instanceof UiPointerFeedback pointer) {
            pointer.pointerScroll(vertical != 0 ? vertical : horizontal);
        }
    }

    public static void renderGame(GuiGraphics graphics) {
        if (Minecraft.getInstance().screen == null) SharedHudHosts.renderOthers(graphics);
    }

    public static void renderScreen(Screen screen, GuiGraphics graphics) {
        if (screen == Minecraft.getInstance().screen) SharedHudHosts.renderOthers(graphics);
    }
}
