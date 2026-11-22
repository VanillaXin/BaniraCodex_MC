package xin.vanilla.banira.client.gui.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;
import xin.vanilla.banira.common.util.CompactCount;
import xin.vanilla.banira.internal.client.ui.NativeGuiGraphics;

public final class IconBadge {
    private IconBadge() {
    }

    public static void draw(Matrix3x2fStack pose, Font font, String amount, int x, int y, int size) {
        NativeGuiGraphics.draw(pose, graphics -> draw(graphics, font, amount, x, y, size));
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, String amount, int x, int y, int size) {
        String text = CompactCount.format(amount);
        if (text.isEmpty() || size <= 0) return;
        int width = font.width(text);
        float scale = Math.min(1F, (float) size / Math.max(1, width));
        Matrix3x2fStack pose = graphics.pose();
        // Item models are extracted separately; the next stratum orders text after them.
        graphics.nextStratum();
        pose.pushMatrix();
        try {
            pose.translate(x + size - width * scale, y + size - font.lineHeight * scale);
            pose.scale(scale, scale);
            graphics.text(font, text, 0, 0, 0xFFFFFFFF, true);
        } finally {
            pose.popMatrix();
            graphics.nextStratum();
        }
    }
}
