package xin.vanilla.banira.internal.client;

import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nonnull;

/**
 * 当前分支的原生绘制对象只保存在这里，公共事件不再向子 mod 暴露它。
 */
public final class PlatformBaniraDrawHandle implements BaniraDrawHandle {
    private final @Nonnull Object nativeGraphics;

    public PlatformBaniraDrawHandle(@Nonnull Object nativeGraphics) {
        this.nativeGraphics = nativeGraphics;
    }

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        BaniraPlatforms.get().renderService().fill(nativeGraphics, x, y, width, height, argb);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        BaniraPlatforms.get().renderService().line(nativeGraphics, x1, y1, x2, y2, lineWidth, argb);
    }

    @Override
    public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        BaniraPlatforms.get().renderService().roundedRect(nativeGraphics, x, y, width, height, argb, radius);
    }

    @Override
    public void text(@Nonnull String text, int x, int y, int argb, boolean shadow) {
        BaniraPlatforms.get().renderService().text(nativeGraphics, text, x, y, argb, shadow);
    }

    @Override
    public void texture(@Nonnull ResourceLocation texture, int x, int y, int width, int height,
                        float u, float v, int textureWidth, int textureHeight) {
        BaniraPlatforms.get().renderService().texture(nativeGraphics, texture, x, y, width, height, u, v, textureWidth, textureHeight);
    }
}
