package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

import javax.annotation.Nonnull;

/**
 * 1.16.5 MatrixStack 绘制适配器；子 mod 只接触 api.client.render。
 */
public final class BaniraLegacyDrawHandle implements BaniraDrawHandle {
    private final Object nativeContext;

    public BaniraLegacyDrawHandle(Object nativeContext) {
        this.nativeContext = nativeContext;
    }

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        BaniraClientAccess.fill(nativeContext, x, y, width, height, argb);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        MatrixStack stack = matrixStack();
        if (stack != null) {
            AbstractGuiUtils.drawLine(stack, x1, y1, x2, y2, lineWidth, argb);
        }
    }

    @Override
    public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        MatrixStack stack = matrixStack();
        if (stack != null) {
            AbstractGuiUtils.drawRoundedRect(stack, x, y, width, height, argb, radius);
        }
    }

    @Override
    public void text(@Nonnull String text, int x, int y, int argb, boolean shadow) {
        BaniraClientAccess.drawText(nativeContext, text, x, y, argb, shadow);
    }

    @Override
    public void texture(@Nonnull String textureId, int x, int y, int width, int height,
                        float u, float v, int textureWidth, int textureHeight) {
        ResourceLocation texture = ResourceLocation.tryParse(textureId);
        if (texture != null) {
            BaniraClientAccess.blit(nativeContext, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }

    private MatrixStack matrixStack() {
        return nativeContext instanceof MatrixStack ? (MatrixStack) nativeContext : null;
    }
}
