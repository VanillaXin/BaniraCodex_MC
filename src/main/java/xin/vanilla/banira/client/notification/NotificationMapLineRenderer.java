package xin.vanilla.banira.client.notification;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.client.util.ClientThemeManager;

/**
 * Legacy maps expose a text region rather than a registration API.
 */
public final class NotificationMapLineRenderer {
    private NotificationMapLineRenderer() {
    }

    public static void drawCentered(MatrixStack stack, ITextComponent text, float centerX, float y, float width) {
        Minecraft mc = Minecraft.getInstance();
        drawCentered(stack, text, centerX, y, width, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    public static void drawCentered(MatrixStack stack, ITextComponent text, float centerX, float y, float width,
                                    int screenWidth, int screenHeight) {
        if (text.getString().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(text);
        float scale = Math.min(.75f, width / Math.max(1, textWidth));
        float drawY = Math.max(0, Math.min(y, screenHeight - 9 * scale));
        float left = Math.max(0, Math.min(centerX - textWidth * scale / 2,
                screenWidth - textWidth * scale));
        stack.pushPose();
        try {
            stack.translate(left, drawY, 0);
            stack.scale(scale, scale, 1);
            mc.font.draw(stack, text, 0, 0, ClientThemeManager.getEffectiveTheme().popupItemText());
        } finally {
            stack.popPose();
        }
    }
}
