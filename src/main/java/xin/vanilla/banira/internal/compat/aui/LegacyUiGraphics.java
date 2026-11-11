package xin.vanilla.banira.internal.compat.aui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

/**
 * Buffer and clipping operations for the MatrixStack GUI renderer.
 */
public final class LegacyUiGraphics {
    private LegacyUiGraphics() {
    }

    public static IRenderTypeBuffer.Impl buffers() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static void flush() {
        buffers().endBatch();
    }

    public static int drawString(com.mojang.blaze3d.matrix.MatrixStack pose, net.minecraft.client.gui.FontRenderer font,
                                 String text, float x, float y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public static int drawString(com.mojang.blaze3d.matrix.MatrixStack pose, net.minecraft.client.gui.FontRenderer font,
                                 net.minecraft.util.IReorderingProcessor text, float x, float y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public static int drawString(com.mojang.blaze3d.matrix.MatrixStack pose, net.minecraft.client.gui.FontRenderer font,
                                 net.minecraft.util.text.ITextComponent text, float x, float y, int color, boolean shadow) {
        return shadow ? font.drawShadow(pose, text, x, y, color) : font.draw(pose, text, x, y, color);
    }

    public static void pushScissor(int left, int top, int right, int bottom) {
        flush();
        AbstractGuiUtils.pushScissor(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    public static void popScissor() {
        flush();
        AbstractGuiUtils.popScissor();
    }
}
