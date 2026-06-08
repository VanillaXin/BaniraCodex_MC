package xin.vanilla.banira.internal.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

import javax.annotation.Nonnull;

/**
 * Forge 1.18.2 的绘制句柄；PoseStack 只停留在加载器适配层。
 */
public final class ForgeBaniraDrawHandle implements BaniraDrawHandle {
    private final @Nonnull PoseStack poseStack;

    public ForgeBaniraDrawHandle(@Nonnull PoseStack poseStack) {
        this.poseStack = poseStack;
    }

    @Override
    public void fill(int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.fill(poseStack, x, y, width, height, argb);
    }

    @Override
    public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        AbstractGuiUtils.drawLine(poseStack, x1, y1, x2, y2, lineWidth, argb);
    }

    @Override
    public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        AbstractGuiUtils.drawRoundedRect(poseStack, x, y, width, height, argb, radius);
    }

    @Override
    public void text(@Nonnull String text, int x, int y, int argb, boolean shadow) {
        Font font = AbstractGuiUtils.getFont();
        Text drawText = Text.literal(text).stack(poseStack).font(font).color(argb).shadow(shadow);
        LabelWidget.drawLimitedText(FontDrawArgs.of(drawText).x(x).y(y).position(EnumEllipsisPosition.NONE).wrap(false));
    }

    @Override
    public void texture(@Nonnull ResourceLocation texture, int x, int y, int width, int height,
                        float u, float v, int textureWidth, int textureHeight) {
        AbstractGuiUtils.blit(poseStack, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }
}
