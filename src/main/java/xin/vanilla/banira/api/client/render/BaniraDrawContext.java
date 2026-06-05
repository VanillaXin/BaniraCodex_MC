package xin.vanilla.banira.api.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.hud.BaniraHudBounds;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

import javax.annotation.Nonnull;

/**
 * 子 mod 绘制 HUD/GUI 的稳定入口；不同 MC 版本在内部转换到 PoseStack/GuiGraphics。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraDrawContext {
    private final @Nonnull Object nativeGraphics;
    private final int screenWidth;
    private final int screenHeight;
    private final float partialTick;

    public BaniraDrawContext(@Nonnull Object nativeGraphics, int screenWidth, int screenHeight, float partialTick) {
        this.nativeGraphics = nativeGraphics;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.partialTick = partialTick;
    }

    public void fill(int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.fill(poseStack(), x, y, width, height, argb);
    }

    public void fill(@Nonnull BaniraHudBounds bounds, int argb) {
        if (bounds.isKnown()) {
            fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), argb);
        }
    }

    public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        AbstractGuiUtils.drawLine(poseStack(), x1, y1, x2, y2, lineWidth, argb);
    }

    public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        AbstractGuiUtils.drawRoundedRect(poseStack(), x, y, width, height, argb, radius);
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
        Font font = Minecraft.getInstance().font;
        Text drawText = Text.literal(text).stack(poseStack()).font(font).color(argb).shadow(shadow);
        LabelWidget.drawLimitedText(FontDrawArgs.of(drawText).x(x).y(y).position(EnumEllipsisPosition.NONE).wrap(false));
    }

    public void texture(@Nonnull ResourceLocation texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        AbstractGuiUtils.blit(poseStack(), texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    private PoseStack poseStack() {
        if (nativeGraphics instanceof PoseStack) {
            return (PoseStack) nativeGraphics;
        }
        throw new IllegalStateException("nativeGraphics is not a PoseStack on this branch: " + nativeGraphics.getClass().getName());
    }
}
