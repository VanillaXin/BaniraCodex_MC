package xin.vanilla.banira.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.api.BaniraEnvironment;

public final class DevFpsOverlay {
    private static final int TEXT_COLOR = 0xFFE8E8E8;

    private DevFpsOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!shouldRender(BaniraEnvironment.isDevelopment(), minecraft.options.hideGui, minecraft.getDebugOverlay().showDebugScreen())) {
            return;
        }
        graphics.drawString(minecraft.font, format(minecraft.getFps()), 2, 2, TEXT_COLOR, true);
    }

    static boolean shouldRender(boolean development, boolean hideGui, boolean debugOverlay) {
        return development && !hideGui && !debugOverlay;
    }

    static String format(int fps) {
        return "FPS: " + Math.max(0, fps);
    }
}
