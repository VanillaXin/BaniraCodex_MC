package xin.vanilla.banira.platform;

import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * 当前分支的基础绘制适配服务。
 */
public interface BaniraRenderService {
    void fill(@Nonnull Object nativeGraphics, int x, int y, int width, int height, int argb);

    void line(@Nonnull Object nativeGraphics, float x1, float y1, float x2, float y2, float lineWidth, int argb);

    void roundedRect(@Nonnull Object nativeGraphics, int x, int y, int width, int height, int argb, int radius);

    void text(@Nonnull Object nativeGraphics, @Nonnull String text, int x, int y, int argb, boolean shadow);

    void texture(@Nonnull Object nativeGraphics, @Nonnull ResourceLocation texture, int x, int y,
                 int width, int height, float u, float v, int textureWidth, int textureHeight);
}
