package xin.vanilla.banira.platform;

import net.minecraft.resources.ResourceLocation;

/**
 * 测试用渲染服务，平台单元测试不执行实际绘制。
 */
public enum NoopRenderService implements BaniraRenderService {
    INSTANCE;

    @Override
    public void fill(Object nativeGraphics, int x, int y, int width, int height, int argb) {
    }

    @Override
    public void line(Object nativeGraphics, float x1, float y1, float x2, float y2, float lineWidth, int argb) {
    }

    @Override
    public void roundedRect(Object nativeGraphics, int x, int y, int width, int height, int argb, int radius) {
    }

    @Override
    public void text(Object nativeGraphics, String text, int x, int y, int argb, boolean shadow) {
    }

    @Override
    public void texture(Object nativeGraphics, ResourceLocation texture, int x, int y,
                        int width, int height, float u, float v, int textureWidth, int textureHeight) {
    }
}
