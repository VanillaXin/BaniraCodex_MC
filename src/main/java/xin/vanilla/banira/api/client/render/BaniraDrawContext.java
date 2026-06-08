package xin.vanilla.banira.api.client.render;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.hud.BaniraHudBounds;

import javax.annotation.Nonnull;

/**
 * 子 mod 绘制 HUD/GUI 的稳定入口；不同 MC 版本在内部转换到 PoseStack/GuiGraphics。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraDrawContext {
    private final @Nonnull BaniraDrawHandle handle;
    private final int screenWidth;
    private final int screenHeight;
    private final float partialTick;

    public BaniraDrawContext(@Nonnull BaniraDrawHandle handle, int screenWidth, int screenHeight, float partialTick) {
        this.handle = handle;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.partialTick = partialTick;
    }

    public void fill(int x, int y, int width, int height, int argb) {
        handle.fill(x, y, width, height, argb);
    }

    public void fill(@Nonnull BaniraHudBounds bounds, int argb) {
        if (bounds.isKnown()) {
            fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), argb);
        }
    }

    public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        handle.line(x1, y1, x2, y2, lineWidth, argb);
    }

    public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        handle.roundedRect(x, y, width, height, argb, radius);
    }

    public void roundedRect(@Nonnull BaniraHudBounds bounds, int argb, int radius) {
        if (bounds.isKnown()) {
            roundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), argb, radius);
        }
    }

    public void progressBar(@Nonnull BaniraHudBounds bounds, float progress, int backgroundArgb, int fillArgb) {
        if (!bounds.isKnown()) {
            return;
        }
        fill(bounds, backgroundArgb);
        int filledWidth = Math.max(0, bounds.progressX(progress) - bounds.x());
        if (filledWidth > 0) {
            fill(bounds.x(), bounds.y(), filledWidth, bounds.height(), fillArgb);
        }
    }

    public void progressMarker(@Nonnull BaniraHudBounds bounds, float progress, int width, int height, int argb) {
        if (!bounds.isKnown()) {
            return;
        }
        int markerWidth = Math.max(1, width);
        int markerHeight = Math.max(1, height);
        int x = bounds.progressX(progress) - markerWidth / 2;
        int y = bounds.y() + (bounds.height() - markerHeight) / 2;
        fill(x, y, markerWidth, markerHeight, argb);
    }

    public void text(@Nonnull String text, int x, int y, int argb) {
        text(text, x, y, argb, false);
    }

    public void text(@Nonnull String text, int x, int y, int argb, boolean shadow) {
        handle.text(text, x, y, argb, shadow);
    }

    public void texture(@Nonnull ResourceLocation texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        handle.texture(texture, x, y, width, height, u, v, textureWidth, textureHeight);
    }
}
