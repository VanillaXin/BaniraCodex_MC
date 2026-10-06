package xin.vanilla.banira.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import xin.vanilla.banira.client.util.ClientThemeManager;

/**
 * Renders inside native maps that expose geometry without an information-slot API.
 */
public final class NotificationMapLineRenderer {
    private NotificationMapLineRenderer() {
    }

    public static void drawCentered(GuiGraphics graphics, Component text, float centerX, float y, float width,
                                    int screenWidth, int screenHeight) {
        if (text.getString().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        float scale = Math.min(.75f, width / Math.max(1, textWidth));
        float drawY = Math.max(0, Math.min(y, screenHeight - 9 * scale));
        float left = Math.max(0, Math.min(centerX - textWidth * scale / 2, screenWidth - textWidth * scale));
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(left, drawY, 0);
            graphics.pose().scale(scale, scale, 1);
            graphics.drawString(mc.font, text, 0, 0, ClientThemeManager.getEffectiveTheme().popupItemText(), false);
        } finally {
            graphics.pose().popPose();
        }
    }
}
