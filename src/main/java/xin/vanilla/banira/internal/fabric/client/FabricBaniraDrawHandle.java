package xin.vanilla.banira.internal.fabric.client;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.internal.client.ui.NativeGuiGraphics;

import javax.annotation.Nonnull;

/**
 * Fabric GUI 提取阶段的绘制句柄。
 */
public final class FabricBaniraDrawHandle implements BaniraDrawHandle {
    private final @Nonnull GuiGraphicsExtractor graphics;

    public FabricBaniraDrawHandle(@Nonnull GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public Matrix3x2fStack nativePose() {
        return NativeGuiGraphics.bind(graphics);
    }

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + height, argb);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        try (var scope = xin.vanilla.banira.client.util.ExtractedGuiRenderer.bind(graphics)) {
            AbstractGuiUtils.drawLine(nativePose(), x1, y1, x2, y2, lineWidth, argb);
        }
    }

    @Override
    public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        try (var scope = xin.vanilla.banira.client.util.ExtractedGuiRenderer.bind(graphics)) {
            AbstractGuiUtils.drawRoundedRect(nativePose(), x, y, width, height, argb, radius);
        }
    }

    @Override
    public void text(@Nonnull String text, int x, int y, int argb, boolean shadow) {
        graphics.text(AbstractGuiUtils.getFont(), text, x, y, argb, shadow);
    }

    @Override
    public void texture(@Nonnull String textureId, int x, int y, int width, int height,
                        float u, float v, int textureWidth, int textureHeight) {
        Identifier texture = Identifier.tryParse(textureId);
        if (texture != null) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }
}
