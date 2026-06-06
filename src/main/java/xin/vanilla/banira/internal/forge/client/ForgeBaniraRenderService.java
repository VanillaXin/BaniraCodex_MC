package xin.vanilla.banira.internal.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.platform.BaniraRenderService;

import javax.annotation.Nonnull;

/**
 * Forge 1.18.2 的基础绘制服务，集中处理 PoseStack 适配。
 */
public final class ForgeBaniraRenderService implements BaniraRenderService {
    public static final ForgeBaniraRenderService INSTANCE = new ForgeBaniraRenderService();

    private ForgeBaniraRenderService() {
    }

    @Override
    public void fill(@Nonnull Object nativeGraphics, int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.fill(poseStack(nativeGraphics), x, y, width, height, argb);
    }

    @Override
    public void line(@Nonnull Object nativeGraphics, float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        AbstractGuiUtils.drawLine(poseStack(nativeGraphics), x1, y1, x2, y2, lineWidth, argb);
    }

    @Override
    public void roundedRect(@Nonnull Object nativeGraphics, int x, int y, int width, int height, int argb, int radius) {
        AbstractGuiUtils.drawRoundedRect(poseStack(nativeGraphics), x, y, width, height, argb, radius);
    }

    @Override
    public void text(@Nonnull Object nativeGraphics, @Nonnull String text, int x, int y, int argb, boolean shadow) {
        PoseStack stack = poseStack(nativeGraphics);
        Font font = Minecraft.getInstance().font;
        Text drawText = Text.literal(text).stack(stack).font(font).color(argb).shadow(shadow);
        LabelWidget.drawLimitedText(FontDrawArgs.of(drawText).x(x).y(y).position(EnumEllipsisPosition.NONE).wrap(false));
    }

    @Override
    public void texture(@Nonnull Object nativeGraphics, @Nonnull ResourceLocation texture, int x, int y,
                        int width, int height, float u, float v, int textureWidth, int textureHeight) {
        AbstractGuiUtils.blit(poseStack(nativeGraphics), texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    private static PoseStack poseStack(@Nonnull Object nativeGraphics) {
        if (nativeGraphics instanceof PoseStack) {
            return (PoseStack) nativeGraphics;
        }
        throw new IllegalStateException("nativeGraphics is not a PoseStack on this branch: " + nativeGraphics.getClass().getName());
    }
}
