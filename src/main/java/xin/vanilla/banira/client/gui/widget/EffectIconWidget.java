package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.data.BaniraColorConfig;
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
    private MobEffectInstance effectInstance;
    private String cachedEffectId;
    private MobEffectInstance cachedEffectIdInstance;
    private String cachedTooltipKey;
    private String cachedTooltipString;
    private Text cachedTooltipText;
    private net.minecraft.network.chat.Component cachedVanillaTooltip;

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

    public EffectIconWidget(BaniraScreen screen, ScreenCoordinate bounds, MobEffectInstance effectInstance) {
        super(screen, bounds);
        this.effectInstance = effectInstance;
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        PoseStack stack = graphics.pose();
        if (!visible) return;
        if (renderCoordinate == null) return;

        MobEffectInstance current = getCurrentMobEffectInstance();
        if (current == null || EffectUtils.isEffectNull(current)) {
            renderChildren(graphics, partialTicks);
            return;
        }

        int x = (int) x();
        int y = (int) y();
        int w = (int) renderCoordinate.width();
        int h = (int) renderCoordinate.height();
        drawEffectIcon(stack, screen.getFont(), current, x, y, w, h, showText);

        renderChildren(graphics, partialTicks);

        if (enableTooltip && mouseInside) {
            int mouseX = (int) screen.inputState().mouseX();
            int mouseY = (int) screen.inputState().mouseY();
            renderTooltip(graphics, stack, mouseX, mouseY, current);
        }
    }

    private void renderTooltip(GuiGraphics graphics, PoseStack stack, int mouseX, int mouseY, MobEffectInstance effectInstance) {
        String tip = tooltipString(effectInstance);
        if (!tip.isEmpty()) {
            stack.pushPose();
            stack.translate(-absoluteX(), -absoluteY(), 0);
            if (screen != null) {
                if (seasonTooltip) {
                    BaniraColorConfig theme = screen.getEffectiveTheme();
                    TooltipWidget.drawPopupMessage(stack,
                            FontDrawArgs.ofPopo(tooltipText(tip).stack(stack).font(screen.getFont()))
                                    .x(mouseX).y(mouseY)
                                    .popupUseTexture(theme.tooltipUseTexture()),
                            theme, screen.season());
                } else {
                    graphics.renderTooltip(screen.getFont(), vanillaTooltip(tip), mouseX, mouseY);
                }
            }
            stack.popPose();
        }
    }

    @Nullable
    private MobEffectInstance getCurrentMobEffectInstance() {
        if (effectInstance != null) return effectInstance;
        if (effectId == null || effectId.isEmpty()) return null;
        if (!effectId.equals(cachedEffectId) || cachedEffectIdInstance == null) {
            cachedEffectId = effectId;
            cachedEffectIdInstance = EffectUtils.deserializeMobEffectInstance(effectId);
        }
        return cachedEffectIdInstance;
    }

    public EffectIconWidget effectId(@Nullable String effectId) {
        this.effectId = effectId;
        this.effectInstance = null;
        invalidateEffectCaches();
        return this;
    }

    public EffectIconWidget effectInstance(@Nullable MobEffectInstance effectInstance) {
        this.effectInstance = effectInstance;
        this.effectId = null;
        invalidateEffectCaches();
        return this;
    }

    private void invalidateEffectCaches() {
        cachedEffectId = null;
        cachedEffectIdInstance = null;
        cachedTooltipKey = null;
        cachedTooltipString = null;
        cachedTooltipText = null;
        cachedVanillaTooltip = null;
    }

    private String tooltipString(MobEffectInstance effect) {
        String key = tooltipKey(effect);
        if (!key.equals(cachedTooltipKey)) {
            cachedTooltipKey = key;
            cachedTooltipString = buildTooltipString(effect);
            cachedTooltipText = null;
            cachedVanillaTooltip = null;
        }
        return cachedTooltipString != null ? cachedTooltipString : "";
    }

    private static String tooltipKey(MobEffectInstance effect) {
        return EffectUtils.getEffectRegistryString(effect) + "|" + effect.getAmplifier() + "|" + effect.getDuration();
    }

    private static String buildTooltipString(MobEffectInstance effect) {
        String duration = effect.getDuration() > 0
                ? DateUtils.toMaxUnitString(effect.getDuration(), DateUtils.DateUnit.SECOND, 0, 1) : "";
        String amplifier = effect.getAmplifier() >= 0 ? NumberUtils.intToRoman(effect.getAmplifier() + 1) : "";
        StringBuilder tip = new StringBuilder(EffectUtils.getEffectDisplayName(effect));
        if (!amplifier.isEmpty()) tip.append(' ').append(amplifier);
        if (!duration.isEmpty()) tip.append('\n').append(duration);
        return tip.toString();
    }

    private Text tooltipText(String tip) {
        if (cachedTooltipText == null || !tip.equals(cachedTooltipText.content())) {
            cachedTooltipText = Text.literal(tip);
        }
        return cachedTooltipText;
    }

    private net.minecraft.network.chat.Component vanillaTooltip(String tip) {
        if (cachedVanillaTooltip == null || !tip.equals(cachedVanillaTooltip.getString())) {
            cachedVanillaTooltip = BaniraComponent.get().literal(tip).toVanilla();
        }
        return cachedVanillaTooltip;
    }


    private static void drawEffectIconText(Font font, PoseStack stack, net.minecraft.network.chat.Component vanilla, float x, float y, int color) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        font.drawInBatch(
                vanilla.getVisualOrderText(),
                x,
                y,
                color,
                false,
                stack.last().pose(),
                bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );
        bufferSource.endBatch();
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
    public static void drawEffectIcon(PoseStack stack, MobEffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
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
    public static void drawEffectIcon(PoseStack stack, Font font, MobEffectInstance effectInstance, int x, int y, int width, int height, boolean showText) {
        ResourceLocation effectIcon = TextureUtils.getEffectTexture(Identifier.id(), effectInstance);
        if (effectIcon != null) {
            AbstractGuiUtils.blit(stack, effectIcon, x, y, 0, 0, width, height, width, height);
        }
        if (showText) {
            // 效果等级
            if (effectInstance.getAmplifier() >= 0) {
                Component amplifierString = BaniraComponent.get().literal(NumberUtils.intToRoman(effectInstance.getAmplifier() + 1));
                int amplifierWidth = font.width(amplifierString.toString());
                float fontX = x + width - (float) amplifierWidth / 2;
                float fontY = y - 1;
                int argb = 0xFFFFFFFF;
                drawEffectIconText(font, stack, amplifierString.color(Color.argb(argb)).toVanilla(), fontX, fontY, argb);
            }
            // 效果持续时间
            if (effectInstance.getDuration() > 0) {
                Component durationString = BaniraComponent.get().literal(DateUtils.toMaxUnitString(effectInstance.getDuration(), DateUtils.DateUnit.SECOND, 0, 1));
                int durationWidth = font.width(durationString.toString());
                float fontX = x + width - (float) durationWidth / 2 - 2;
                float fontY = y + (float) height / 2 + 1;
                int argb = 0xFFFFFFFF;
                drawEffectIconText(font, stack, durationString.color(Color.argb(argb)).toVanilla(), fontX, fontY, argb);
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
    public static void drawEffectIcon(PoseStack stack, MobEffectInstance effectInstance, int x, int y, boolean showText) {
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
    public static void drawEffectIcon(PoseStack stack, Font font, MobEffectInstance effectInstance, int x, int y, boolean showText) {
        drawEffectIcon(stack, font, effectInstance, x, y, AbstractGuiUtils.ITEM_ICON_SIZE, AbstractGuiUtils.ITEM_ICON_SIZE, showText);
    }
}
