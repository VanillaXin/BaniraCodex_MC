package xin.vanilla.banira.internal.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * 客户端绘制后端桥接点；加载器适配层保留当前 GUI 提取器及其原生渲染状态。
 */
public final class BaniraClientDrawBridge {
    private BaniraClientDrawBridge() {
    }

    private static Function<GuiGraphicsExtractor, BaniraDrawHandle> drawHandleFactory;

    public static void install(@Nonnull Function<GuiGraphicsExtractor, BaniraDrawHandle> factory) {
        drawHandleFactory = factory;
    }

    @Nonnull
    public static BaniraDrawHandle handle(@Nonnull GuiGraphicsExtractor graphics) {
        if (drawHandleFactory == null) {
            throw new IllegalStateException("Banira client draw bridge has not been installed.");
        }
        return drawHandleFactory.apply(graphics);
    }
}
