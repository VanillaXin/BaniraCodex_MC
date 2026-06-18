package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * 客户端绘制后端桥接点；加载器适配层负责安装当前版本的 PoseStack 包装器。
 */
public final class BaniraClientDrawBridge {
    private BaniraClientDrawBridge() {
    }

    private static Function<PoseStack, BaniraDrawHandle> drawHandleFactory;

    public static void install(@Nonnull Function<PoseStack, BaniraDrawHandle> factory) {
        drawHandleFactory = factory;
    }

    @Nonnull
    public static BaniraDrawHandle handle(@Nonnull PoseStack poseStack) {
        if (drawHandleFactory == null) {
            throw new IllegalStateException("Banira client draw bridge has not been installed.");
        }
        return drawHandleFactory.apply(poseStack);
    }
}
