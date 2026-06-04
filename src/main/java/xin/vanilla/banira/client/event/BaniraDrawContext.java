package xin.vanilla.banira.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

@Getter
@Accessors(fluent = true)
public final class BaniraDrawContext {
    private final Object nativeContext;
    private final int width;
    private final int height;
    private final float partialTicks;

    public BaniraDrawContext(Object nativeContext, int width, int height, float partialTicks) {
        this.nativeContext = nativeContext;
        this.width = width;
        this.height = height;
        this.partialTicks = partialTicks;
    }

    public <T> T nativeContext(Class<T> type) {
        return type.isInstance(nativeContext) ? type.cast(nativeContext) : null;
    }

    public void fill(int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.fill(stack(), x, y, width, height, argb);
    }

    public int drawText(String text, int x, int y, int argb, boolean shadow) {
        if (text == null) return x;
        if (shadow) {
            return AbstractGuiUtils.getFont().drawShadow(stack(), text, x, y, argb);
        }
        return AbstractGuiUtils.getFont().draw(stack(), text, x, y, argb);
    }

    /**
     * Draws text around a center point without exposing the native font class to child mods.
     */
    public int drawCenteredText(String text, int centerX, int y, int argb, boolean shadow) {
        return drawText(text, centerX - textWidth(text) / 2, y, argb, shadow);
    }

    public int drawRightAlignedText(String text, int rightX, int y, int argb, boolean shadow) {
        return drawText(text, rightX - textWidth(text), y, argb, shadow);
    }

    public int textWidth(String text) {
        return text == null ? 0 : AbstractGuiUtils.getFont().width(text);
    }

    public int lineHeight() {
        return AbstractGuiUtils.getFont().lineHeight;
    }

    public void fillScreen(int argb) {
        fill(0, 0, width, height, argb);
    }

    public void blit(ResourceLocation texture, int x, int y, double u, double v, int width, int height, int textureWidth, int textureHeight) {
        AbstractGuiUtils.blit(stack(), texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    private MatrixStack stack() {
        if (nativeContext instanceof MatrixStack) {
            return (MatrixStack) nativeContext;
        }
        throw new IllegalStateException("Unsupported draw context: " + nativeContext);
    }
}
