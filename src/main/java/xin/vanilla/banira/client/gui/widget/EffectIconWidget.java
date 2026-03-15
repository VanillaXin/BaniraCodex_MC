package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.DateUtils;
import xin.vanilla.banira.common.util.EffectUtils;
import xin.vanilla.banira.common.util.NumberUtils;

import javax.annotation.Nullable;

/**
 * 药水效果图标 Widget，用于绘制效果图标
 */
@Accessors(chain = true, fluent = true)
public class EffectIconWidget extends BaseWidget {
    @Getter
    private String effectId;

    @Getter
    private EffectInstance effectInstance;

    @Getter
    @Setter
    private boolean showText = false;

    @Getter
    @Setter
    private boolean enableTooltip = true;

    @Getter
    @Setter
    private boolean seasonTooltip = true;

    public EffectIconWidget(BaniraScreen screen) {
        super(screen);
    }

    public EffectIconWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public EffectIconWidget(BaniraScreen screen, ScreenCoordinate bounds, String effectId) {
        super(screen, bounds);
        this.effectId = effectId;
    }

    public EffectIconWidget(BaniraScreen screen, ScreenCoordinate bounds, EffectInstance effectInstance) {
        super(screen, bounds);
        this.effectInstance = effectInstance;
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) return;
        if (renderCoordinate == null) return;

        EffectInstance current = getCurrentEffectInstance();
        if (current == null || EffectUtils.isEffectNull(current)) {
            renderChildren(stack, partialTicks);
            return;
        }

        int x = (int) x();
        int y = (int) y();
        int w = (int) renderCoordinate.width();
        int h = (int) renderCoordinate.height();
        drawEffectIcon(stack, screen.getFont(), current, x, y, w, h, showText);

        renderChildren(stack, partialTicks);

        if (enableTooltip && mouseInside) {
            int mouseX = (int) screen.inputState().mouseX();
            int mouseY = (int) screen.inputState().mouseY();
            renderTooltip(stack, mouseX, mouseY, current);
        }
    }

    private void renderTooltip(MatrixStack stack, int mouseX, int mouseY, EffectInstance effectInstance) {
        String displayName = EffectUtils.getEffectDisplayName(effectInstance);
        String duration = effectInstance.getDuration() > 0
                ? DateUtils.toMaxUnitString(effectInstance.getDuration(),
                DateUtils.DateUnit.SECOND, 0, 1)
                : "";
        String amplifier = effectInstance.getAmplifier() >= 0
                ? NumberUtils.intToRoman(effectInstance.getAmplifier() + 1)
                : "";
        StringBuilder tip = new StringBuilder(displayName);
        if (!amplifier.isEmpty()) tip.append(" ").append(amplifier);
        if (!duration.isEmpty()) tip.append("\n").append(duration);
        if (tip.length() > 0) {
            stack.pushPose();
            stack.translate(-absoluteX(), -absoluteY(), 0);
            if (screen != null) {
                if (seasonTooltip) {
                    TooltipWidget.drawPopupMessage(stack,
                            FontDrawArgs.ofPopo(Text.literal(tip.toString()).stack(stack).font(screen.getFont()))
                                    .x(mouseX).y(mouseY),
                            screen.getEffectiveTheme(), screen.season());
                } else {
                    screen.renderTooltip(stack, Component.literal(tip.toString()).toVanilla(), mouseX, mouseY);
                }
            }
            stack.popPose();
        }
    }

    @Nullable
    private EffectInstance getCurrentEffectInstance() {
        if (effectInstance != null) return EffectUtils.copyEffectInstance(effectInstance);
        if (effectId == null || effectId.isEmpty()) return null;
        return EffectUtils.deserializeEffectInstance(effectId);
    }

    public EffectIconWidget effectId(@Nullable String effectId) {
        this.effectId = effectId;
        this.effectInstance = null;
        return this;
    }

    public EffectIconWidget effectInstance(@Nullable EffectInstance effectInstance) {
        this.effectInstance = effectInstance;
        this.effectId = null;
        return this;
    }


    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param width          目标矩形的宽度，决定了图像在屏幕上的宽度
     * @param height         目标矩形的高度，决定了图像在屏幕上的高度
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, EffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
        drawEffectIcon(stack, AbstractGuiUtils.getFont(), effectInstance, x, y, width, height, showText);
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param width          目标矩形的宽度，决定了图像在屏幕上的宽度
     * @param height         目标矩形的高度，决定了图像在屏幕上的高度
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, FontRenderer font, EffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
        ResourceLocation effectIcon = TextureUtils.getEffectTexture(Identifier.id(), effectInstance);
        if (effectIcon != null) {
            AbstractGuiUtils.blit(stack, effectIcon, x, y, 0, 0, width, height, width, height);
        }
        if (showText) {
            // 效果等级
            if (effectInstance.getAmplifier() >= 0) {
                Component amplifierString = Component.literal(NumberUtils.intToRoman(effectInstance.getAmplifier() + 1));
                int amplifierWidth = font.width(amplifierString.toString());
                float fontX = x + width - (float) amplifierWidth / 2;
                float fontY = y - 1;
                int argb = 0xFFFFFFFF;
                font.draw(stack, amplifierString.color(Color.argb(argb)).toVanilla(), fontX, fontY, argb);
            }
            // 效果持续时间
            if (effectInstance.getDuration() > 0) {
                Component durationString = Component.literal(DateUtils.toMaxUnitString(effectInstance.getDuration(), DateUtils.DateUnit.SECOND, 0, 1));
                int durationWidth = font.width(durationString.toString());
                float fontX = x + width - (float) durationWidth / 2 - 2;
                float fontY = y + (float) height / 2 + 1;
                int argb = 0xFFFFFFFF;
                font.draw(stack, durationString.color(Color.argb(argb)).toVanilla(), fontX, fontY, argb);
            }
        }
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, EffectInstance effectInstance, int x, int y, boolean showText) {
        drawEffectIcon(stack, AbstractGuiUtils.getFont(), effectInstance, x, y, showText);
    }

    /**
     * 绘制效果图标
     *
     * @param effectInstance 待绘制的效果实例
     * @param x              矩形的左上角x坐标
     * @param y              矩形的左上角y坐标
     * @param showText       是否显示效果等级和持续时间
     */
    public static void drawEffectIcon(MatrixStack stack, FontRenderer font, EffectInstance effectInstance, int x, int y, boolean showText) {
        drawEffectIcon(stack, font, effectInstance, x, y, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE, showText);
    }
}
