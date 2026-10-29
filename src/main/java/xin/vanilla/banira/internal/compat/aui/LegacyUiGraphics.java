package xin.vanilla.banira.internal.compat.aui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

/** Buffer and clipping operations for the PoseStack GUI renderer. */
public final class LegacyUiGraphics {
    private LegacyUiGraphics() {
    }

    public static MultiBufferSource.BufferSource buffers() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    public static void flush() {
        buffers().endBatch();
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
