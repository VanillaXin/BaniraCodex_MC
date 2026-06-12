package xin.vanilla.banira.api.client.render;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * 绘制后端句柄；各加载器分支负责把这些调用转成当前版本的实际渲染 API。
 */
public interface BaniraDrawHandle {
    void fill(int x, int y, int width, int height, int argb);

    void line(float x1, float y1, float x2, float y2, float lineWidth, int argb);

    void roundedRect(int x, int y, int width, int height, int argb, int radius);

    void text(@Nonnull String text, int x, int y, int argb, boolean shadow);

    void texture(@Nonnull ResourceLocation texture, int x, int y, int width, int height,
                 float u, float v, int textureWidth, int textureHeight);
}
