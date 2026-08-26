package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.enums.EnumTooltipTextureMode;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.tooltip.TooltipBounds;
import xin.vanilla.banira.client.gui.tooltip.TooltipPlacement;
import xin.vanilla.banira.client.gui.tooltip.TooltipRequestCollector;
import xin.vanilla.banira.client.gui.tooltip.TooltipTransitionFrame;
import xin.vanilla.banira.client.gui.tooltip.TooltipTransitionModel;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.ItemUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示Widget。提供 drawPopupMessage 等静态绘制方法。
 * <p>
 * 绘制逻辑：
 * <ul>
 *   <li>可指定是否「使用纹理绘制」（默认 true）</li>
 *   <li>使用纹理绘制为真时：使用纹理绘制；否则使用颜色绘制</li>
 *   <li>指定了季节则使用对应季节的纹理或颜色绘制</li>
 * </ul>
 */
@Accessors(chain = true, fluent = true)
public class TooltipWidget extends BaseWidget implements ITextWidget {
    private static final long TOOLTIP_TRANSITION_NANOS = 140_000_000L;
    private static final long TOOLTIP_CONTINUITY_NANOS = TOOLTIP_TRANSITION_NANOS;
    private static final double TOOLTIP_CONTINUITY_DISTANCE = 56.0D;
    private static final TooltipRequestCollector<PopupRenderData> POPUP_REQUESTS = new TooltipRequestCollector<>();
    private static final TooltipTransitionModel<String> POPUP_TRANSITION =
            new TooltipTransitionModel<>(TOOLTIP_TRANSITION_NANOS, TOOLTIP_CONTINUITY_NANOS,
                    TOOLTIP_CONTINUITY_DISTANCE, 0.35D);
    private static final Map<String, PopupRenderData> POPUP_CONTENT = new LinkedHashMap<>();
    private static Object popupScreenToken;
    private static boolean collectingPopupRequests;
    private static double popupMouseX = Double.NaN;
    private static double popupMouseY = Double.NaN;

    @Getter
    private Text text = Text.empty();

    @Getter
    @Nullable
    private ItemStack itemStack;

    @Getter
    @Setter
    private boolean seasonTooltip = true;

    @Getter
    @Setter
    private boolean vanillaTooltip = false;

    /**
     * 悬浮提示纹理绘制模式。AUTO 时使用主题配置，非 AUTO 时使用本控件定义。
     */
    @Getter
    @Setter
    private EnumTooltipTextureMode popupTextureMode = EnumTooltipTextureMode.AUTO;

    /**
     * 为 true 时弹层使用屏幕坐标绘制（不随父级 translate），避免嵌套时错位。默认 false。
     */
    @Getter
    @Setter
    private boolean popupAtScreenCoords = false;

    private transient final List<net.minecraft.network.chat.Component> tooltip = new ArrayList<>();

    public TooltipWidget(BaniraScreen screen) {
        super(screen);
    }

    public TooltipWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public TooltipWidget(BaniraScreen screen, ScreenCoordinate bounds, Component text) {
        super(screen, bounds);
        this.text = Text.from(text);
    }

    public TooltipWidget(BaniraScreen screen, ScreenCoordinate bounds, Text text) {
        super(screen, bounds);
        this.text = text;
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        PoseStack stack = graphics.pose();
        if (!visible) return;
        if (screen instanceof BaniraScreen && screen.isAnyDropdownSelectOpen()) {
            renderChildren(graphics, partialTicks);
            return;
        }
        if (mouseInside) {
            int mouseX = (int) screen.inputState().mouseX();
            int mouseY = (int) screen.inputState().mouseY();
            if (popupAtScreenCoords) {
                // 延迟到帧末绘制，避免父级 translate 导致错位、scissor 裁剪、层级被覆盖
                BaniraColorConfig theme = screen.getEffectiveTheme();
                EnumSeason season = screen.season();
                Text textToDraw = text;
                boolean useTexture = resolvePopupUseTexture(theme);
                screen.addDeferredTooltipRender(g -> {
                    g.pose().pushPose();
                    g.pose().last().pose().identity();
                    if (itemStack != null && !itemStack.isEmpty()) {
                        drawItemTooltip(g.pose(), itemStack, mouseX, mouseY, seasonTooltip ? screen.season() : null);
                    } else if (vanillaTooltip) {
                        List<net.minecraft.network.chat.Component> tip = new ArrayList<>();
                        tip.add(textToDraw.toComponent().toChat());
                        g.renderTooltip(screen.getFont(), tip, java.util.Optional.empty(), mouseX, mouseY);
                    } else {
                        drawPopupMessage(g.pose(), FontDrawArgs.ofPopo(textToDraw.stack(g.pose())).x(mouseX).y(mouseY).popupUseTexture(useTexture), theme, season);
                    }
                    g.pose().popPose();
                });
            } else {
                stack.pushPose();
                if (parent != null) {
                    stack.translate(-absoluteX(), -absoluteY(), 0);
                }
                if (itemStack != null && !itemStack.isEmpty()) {
                    drawItemTooltip(stack, itemStack, mouseX, mouseY, seasonTooltip ? screen.season() : null);
                } else if (vanillaTooltip) {
                    if (tooltip.isEmpty()) {
                        tooltip.add(text.toComponent().toChat());
                    }
                    graphics.renderTooltip(screen.getFont(), tooltip, java.util.Optional.empty(), mouseX, mouseY);
                } else {
                    BaniraColorConfig theme = screen.getEffectiveTheme();
                    EnumSeason season = screen.season();
                    drawPopupMessage(stack, FontDrawArgs.ofPopo(text.stack(stack)).x(mouseX).y(mouseY).popupUseTexture(resolvePopupUseTexture(theme)), theme, season);
                }
                stack.popPose();
            }
        }
        renderChildren(graphics, partialTicks);
    }

    private boolean resolvePopupUseTexture(BaniraColorConfig theme) {
        switch (popupTextureMode) {
            case TEXTURE:
                return true;
            case COLOR:
                return false;
            default:
                return theme != null && theme.tooltipUseTexture();
        }
    }

    /**
     * 获取指定季节的提示纹理路径
     */
    public static String getSeasonTexturePath(EnumSeason season) {
        season = BaniraColorConfig.resolveEffectiveSeason(season);
        switch (season) {
            case SUMMER:
                return "gui/aotake_cat.png";
            case AUTUMN:
                return "gui/narcissus_cat.png";
            case WINTER:
                return "gui/snowflake_cat.png";
            default:
                return "gui/sakura_cat.png";
        }
    }

    /**
     * 绘制弹出层消息。
     * 渲染规则：popupUseTexture 为真时使用纹理绘制，否则使用颜色绘制；指定季节则使用对应季节的纹理或颜色。
     *
     * @param theme  主题配置，颜色绘制时使用（非空时直接使用，否则按 season 解析季节预设）
     * @param season 季节，纹理绘制时选择季节纹理，颜色绘制时解析季节主题
     */
    public static void drawPopupMessage(PoseStack stack, FontDrawArgs args,
                                        @Nullable BaniraColorConfig theme, @Nullable EnumSeason season) {
        FontDrawArgs drawArgs = args.clone();
        boolean useTextureMode = drawArgs.popupUseTexture();
        BaniraColorConfig resolvedTheme = null;

        if (useTextureMode) {
            useTexture(drawArgs, season);
        } else {
            useColor(drawArgs, theme, season);
            resolvedTheme = resolveTheme(theme, season);
        }

        PopupRenderData request = preparePopupRenderData(drawArgs, resolvedTheme);
        if (collectingPopupRequests) {
            POPUP_REQUESTS.submit(request);
        } else {
            renderPopup(stack, request, request.bounds);
        }
    }

    /** 在屏幕开始绘制时开启本帧 Tooltip 请求收集。 */
    public static void beginPopupFrame(Object screenToken) {
        beginPopupFrame(screenToken, Double.NaN, Double.NaN);
    }

    /** 在屏幕开始绘制时记录当前鼠标位置并开启 Tooltip 请求收集。 */
    public static void beginPopupFrame(Object screenToken, double mouseX, double mouseY) {
        if (popupScreenToken != screenToken) {
            popupScreenToken = screenToken;
            cancelPopupTransition();
        }
        popupMouseX = mouseX;
        popupMouseY = mouseY;
        POPUP_REQUESTS.beginFrame(screenToken);
        collectingPopupRequests = true;
    }

    /** 点击或切换界面时立即取消悬浮提示连续状态。 */
    public static void cancelPopupTransition() {
        POPUP_TRANSITION.reset();
        POPUP_CONTENT.clear();
    }

    /** 在所有屏幕浮层完成后，只绘制本帧视觉层级最高的 Tooltip。 */
    public static void flushPopupFrame(PoseStack stack) {
        flushPopupFrame(stack, true);
    }

    /**
     * 刷新晚于默认屏幕后置事件提交的 Tooltip；本轮没有提交时不推进消失动画。
     */
    public static void flushSubmittedPopupFrame(PoseStack stack) {
        flushPopupFrame(stack, false);
    }

    /** 高版本渲染回调可直接提交 GuiGraphics，无需调用方拆取矩阵栈。 */
    public static void flushSubmittedPopupFrame(GuiGraphics graphics) {
        flushSubmittedPopupFrame(graphics.pose());
    }

    private static void flushPopupFrame(PoseStack stack, boolean resolveMissing) {
        collectingPopupRequests = false;
        long now = System.nanoTime();
        if (!POPUP_REQUESTS.hasWinner()) {
            if (!resolveMissing) return;
            TooltipTransitionFrame<String> frame = POPUP_TRANSITION.resolveMissing(popupMouseX, popupMouseY, now);
            if (frame == null) {
                POPUP_CONTENT.clear();
                return;
            }
            renderTransitionFrame(stack, frame, null);
            return;
        }

        PopupRenderData target = POPUP_REQUESTS.winner();
        POPUP_CONTENT.put(target.contentKey, target);
        TooltipTransitionFrame<String> frame = POPUP_TRANSITION.resolve(
                target.contentKey, target.bounds, popupMouseX, popupMouseY, now);
        renderTransitionFrame(stack, frame, target);

        if (frame.progress() >= 1.0D) {
            POPUP_CONTENT.clear();
            POPUP_CONTENT.put(target.contentKey, target);
        }
    }

    private static void renderTransitionFrame(PoseStack stack, TooltipTransitionFrame<String> frame,
                                              @Nullable PopupRenderData fallback) {
        if (frame.bounds().width() < 1.0D || frame.bounds().height() < 1.0D) return;
        PopupRenderData visible = POPUP_CONTENT.get(frame.contentKey());
        if (visible == null) visible = fallback;
        if (visible == null) return;

        stack.pushPose();
        try {
            stack.last().pose().identity();
            renderPopup(stack, visible, frame.bounds());
        } finally {
            stack.popPose();
        }
    }

    private static void useTexture(FontDrawArgs drawArgs, @Nullable EnumSeason season) {
        if (drawArgs.texture() == null) {
            EnumSeason s = BaniraColorConfig.resolveEffectiveSeason(season);
            drawArgs.texture(Texture.of(TextureUtils.loadCustomTexture(Identifier.id(), getSeasonTexturePath(s))));
        }
        drawArgs.bgArgb(0).bgBorderRadius(0).bgBorderThickness(0);
    }

    private static void useColor(FontDrawArgs drawArgs, @Nullable BaniraColorConfig theme, @Nullable EnumSeason season) {
        BaniraColorConfig resolved = resolveTheme(theme, season);
        drawArgs.bgArgb(resolved.popupBg()).bgBorderRadius(2).bgBorderThickness(1).texture(null);
        drawArgs.text().color(Color.argb(resolved.textPrimary()));
    }

    private static BaniraColorConfig resolveTheme(@Nullable BaniraColorConfig theme, @Nullable EnumSeason season) {
        if (theme != null) return theme;
        return BaniraColorConfig.forSeason(season);
    }

    /**
     * 使用当前季节颜色绘制
     */
    public static void drawPopupMessageWithSeason(PoseStack stack, FontDrawArgs args) {
        drawPopupMessage(stack, args.clone(), null, null);
    }

    /**
     * 使用当前季节纹理绘制
     */
    public static void drawPopupMessageWithSeasonTexture(PoseStack stack, FontDrawArgs args) {
        drawPopupMessageWithSeasonTexture(stack, args, EnumSeason.AUTO);
    }

    /**
     * 使用指定季节纹理绘制
     */
    public static void drawPopupMessageWithSeasonTexture(PoseStack stack, FontDrawArgs args, EnumSeason season) {
        FontDrawArgs drawArgs = args.clone().popupUseTexture(true);
        drawPopupMessage(stack, drawArgs, null, season);
    }

    /**
     * 使用默认样式绘制
     */
    public static void drawPopupMessage(PoseStack stack, FontDrawArgs args) {
        drawPopupMessage(stack, args.clone(), null, null);
    }

    /**
     * 绘制物品提示
     *
     * @param season 季节，非 null 时使用该季节的主题纹理；null 时使用默认样式
     */
    public static void drawItemTooltip(PoseStack stack, ItemStack itemStack, double x, double y, @Nullable EnumSeason season) {
        boolean advanced = Screen.hasShiftDown();
        List<Component> tooltipList = ItemUtils.getItemTooltip(itemStack, Minecraft.getInstance().player, advanced);
        Component tooltipComponent = BaniraComponent.get().empty();
        for (int idx = 0; idx < tooltipList.size(); idx++) {
            Component component = tooltipList.get(idx);
            if (idx > 0) tooltipComponent = tooltipComponent.append("\n");
            tooltipComponent = tooltipComponent.append(component);
        }
        Text tooltipText = new Text(tooltipComponent);
        Font font = Minecraft.getInstance().font;
        FontDrawArgs drawArgs = FontDrawArgs.ofPopo(tooltipText.stack(stack).font(font)).x(x).y(y);
        if (season != null) {
            drawPopupMessageWithSeasonTexture(stack, drawArgs, season);
        } else {
            drawPopupMessage(stack, drawArgs);
        }
    }

    /**
     * 绘制物品提示
     */
    public static void drawItemTooltip(PoseStack stack, ItemStack itemStack, double x, double y, boolean season) {
        drawItemTooltip(stack, itemStack, x, y, season ? EnumSeason.AUTO : null);
    }

    private static PopupRenderData preparePopupRenderData(FontDrawArgs args, @Nullable BaniraColorConfig theme) {
        boolean useThemeColor = (theme != null);
        float calculatedTextureScale = 1.0f;
        int calculatedPaddingLeft;
        int calculatedPaddingRight;
        int calculatedPaddingTop;
        int calculatedPaddingBottom;
        if (args.popupPaddingAuto()) {
            if (useThemeColor) {
                calculatedPaddingLeft = FontDrawArgs.getPopupPaddingLeft();
                calculatedPaddingRight = FontDrawArgs.getPopupPaddingRight();
                calculatedPaddingTop = FontDrawArgs.getPopupPaddingTop();
                calculatedPaddingBottom = FontDrawArgs.getPopupPaddingBottom();
            } else {
                calculatedPaddingLeft = 0;
                calculatedPaddingRight = 0;
                calculatedPaddingTop = 0;
                calculatedPaddingBottom = 0;
            }
        } else {
            calculatedPaddingLeft = args.paddingLeft();
            calculatedPaddingRight = args.paddingRight();
            calculatedPaddingTop = args.paddingTop();
            calculatedPaddingBottom = args.paddingBottom();
        }

        FontDrawArgs calcArgs = args.clone()
                .paddingLeft(calculatedPaddingLeft).paddingRight(calculatedPaddingRight)
                .paddingTop(calculatedPaddingTop).paddingBottom(calculatedPaddingBottom);
        KeyValue<Integer, Integer> textSize = LabelWidget.calculateLimitedTextSize(calcArgs);
        int textWidth = textSize.key();
        int textHeight = textSize.val();

        final TextureUtils.NinePatchInfo ninePatchInfo = args.texture() != null ? TextureUtils.parseNinePatch(args.texture()) : null;

        if (ninePatchInfo != null) {
            Color color = Color.argb(ninePatchInfo.textColor);
            if (!color.isEmpty()) args.text().color(color);
            Font font = args.text().font();
            float targetFontSize = args.fontSize() > 0 ? args.fontSize() : font.lineHeight;
            if (ninePatchInfo.rightGuideHeight > 0) {
                calculatedTextureScale = targetFontSize / ninePatchInfo.rightGuideHeight;
            }
            if (ninePatchInfo.bottomGuideLeftPadding > 0)
                calculatedPaddingLeft += (int) (ninePatchInfo.bottomGuideLeftPadding * calculatedTextureScale);
            if (ninePatchInfo.bottomGuideRightPadding > 0)
                calculatedPaddingRight += (int) (ninePatchInfo.bottomGuideRightPadding * calculatedTextureScale);
            if (ninePatchInfo.rightGuideTopPadding > 0)
                calculatedPaddingTop += (int) (ninePatchInfo.rightGuideTopPadding * calculatedTextureScale);
            if (ninePatchInfo.rightGuideBottomPadding > 0)
                calculatedPaddingBottom += (int) (ninePatchInfo.rightGuideBottomPadding * calculatedTextureScale);
            FontDrawArgs recalcArgs = args.clone()
                    .paddingLeft(calculatedPaddingLeft).paddingRight(calculatedPaddingRight)
                    .paddingTop(calculatedPaddingTop).paddingBottom(calculatedPaddingBottom);
            textSize = LabelWidget.calculateLimitedTextSize(recalcArgs);
            textWidth = textSize.key();
            textHeight = textSize.val();
        }

        final int finalCalculatedPaddingLeft = calculatedPaddingLeft;
        final int finalCalculatedPaddingRight = calculatedPaddingRight;
        final int finalCalculatedPaddingTop = calculatedPaddingTop;
        final int finalCalculatedPaddingBottom = calculatedPaddingBottom;
        int msgWidth = textWidth;
        int msgHeight = textHeight;
        double adjustedX = args.x();
        double adjustedY = args.y();
        int finalMaxWidth = args.maxWidth();

        if (args.inScreen()) {
            KeyValue<Integer, Integer> screenSize = AbstractGuiUtils.getScreenSize();
            int screenWidth = screenSize.key();
            int screenHeight = screenSize.val();

            if (args.wrap()) {
                int effectiveMaxWidth = finalMaxWidth > 0 ? finalMaxWidth : Math.max(0, screenWidth - args.marginLeft() - args.marginRight());
                if (effectiveMaxWidth > 0) {
                    FontDrawArgs maxWidthRecalcArgs = args.clone()
                            .paddingLeft(finalCalculatedPaddingLeft).paddingRight(finalCalculatedPaddingRight)
                            .paddingTop(finalCalculatedPaddingTop).paddingBottom(finalCalculatedPaddingBottom)
                            .maxWidth(effectiveMaxWidth);
                    KeyValue<Integer, Integer> maxWidthTextSize = LabelWidget.calculateLimitedTextSize(maxWidthRecalcArgs);
                    msgWidth = maxWidthTextSize.key();
                    msgHeight = maxWidthTextSize.val();
                    if (finalMaxWidth <= 0) finalMaxWidth = effectiveMaxWidth;
                }
            }

            TooltipBounds placement = TooltipPlacement.place(
                    args.x(), args.y(), msgWidth, msgHeight,
                    screenWidth, screenHeight,
                    args.marginLeft(), args.marginRight(), args.marginTop(), args.marginBottom());
            adjustedX = placement.x();
            adjustedY = placement.y();

            if (args.wrap()) {
                int actualAvailableWidth = screenWidth - (int) adjustedX - args.marginRight();
                if (finalMaxWidth > 0) actualAvailableWidth = Math.min(actualAvailableWidth, finalMaxWidth);
                actualAvailableWidth = Math.max(actualAvailableWidth, finalCalculatedPaddingLeft + finalCalculatedPaddingRight);
                finalMaxWidth = actualAvailableWidth;
            }
        }

        String contentKey = args.text().content(false) + '\u0000'
                + args.text().font().getClass().getName() + '\u0000' + args.fontSize();
        return new PopupRenderData(
                contentKey,
                args,
                theme,
                useThemeColor,
                ninePatchInfo,
                calculatedTextureScale,
                calculatedPaddingLeft,
                calculatedPaddingRight,
                calculatedPaddingTop,
                calculatedPaddingBottom,
                finalMaxWidth,
                new TooltipBounds(adjustedX, adjustedY, msgWidth, msgHeight)
        );
    }

    private static void renderPopup(PoseStack stack, PopupRenderData data, TooltipBounds bounds) {
        FontDrawArgs args = data.args;
        int drawX = (int) Math.round(bounds.x());
        int drawY = (int) Math.round(bounds.y());
        int drawWidth = Math.max(1, (int) Math.round(bounds.width()));
        int drawHeight = Math.max(1, (int) Math.round(bounds.height()));
        args.text().stack(stack);

        AbstractGuiUtils.renderByDepth(stack, EnumRenderDepth.TOOLTIP, (s) -> {
            if (args.texture() != null && data.ninePatchInfo != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                NinePatchImageWidget.drawNinePatch(s, args.texture(), drawX, drawY, drawWidth, drawHeight, data.textureScale);
                AbstractGuiUtils.restoreGuiRenderState();
            } else if (args.texture() != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                Texture tex = args.texture();
                ImageWidget.blit(s, tex, drawX, drawY, drawWidth, drawHeight);
                AbstractGuiUtils.restoreGuiRenderState();
            } else if (data.useThemeColor && data.theme != null) {
                int radius = args.bgBorderRadius();
                int borderThickness = args.bgBorderThickness();
                ShapeDrawArgs.RoundedCornerMode cornerMode = args.popupCornerMode() != null ? args.popupCornerMode() : ShapeDrawArgs.RoundedCornerMode.FINE;
                ShapeDrawArgs fillArgs = ShapeDrawArgs.rect(s, drawX, drawY, drawWidth, drawHeight, data.theme.popupBg());
                fillArgs.rect().radius(radius).cornerMode(cornerMode);
                BaseShapeWidget.drawShape(fillArgs);
                if (borderThickness > 0) {
                    ShapeDrawArgs borderArgs = ShapeDrawArgs.rect(s, drawX, drawY, drawWidth, drawHeight, data.theme.popupBorder());
                    borderArgs.rect().radius(radius).border(borderThickness).cornerMode(cornerMode);
                    BaseShapeWidget.drawShape(borderArgs);
                }
            } else {
                int borderRadius = args.bgBorderRadius();
                int borderThickness = args.bgBorderThickness();
                AbstractGuiUtils.drawRoundedRect(s, drawX, drawY, drawWidth, drawHeight, args.bgArgb(), borderRadius);
                int borderArgb = ColorUtils.softenArgb(args.bgArgb());
                AbstractGuiUtils.drawRoundedRectOutLine(s,
                        drawX, drawY,
                        drawWidth, drawHeight,
                        borderRadius, borderRadius, borderRadius, borderRadius,
                        borderThickness, borderArgb,
                        ShapeDrawArgs.RoundedCornerMode.FINE);
            }

            FontDrawArgs clone = args.clone()
                    .x(drawX).y(drawY)
                    .bgArgb(0x00000000).position(EnumEllipsisPosition.MIDDLE)
                    .paddingLeft(data.paddingLeft).paddingRight(data.paddingRight)
                    .paddingTop(data.paddingTop).paddingBottom(data.paddingBottom);
            if (args.wrap() && data.maxWidthForText > 0) clone.maxWidth(data.maxWidthForText);
            else if (args.maxWidth() > 0) clone.maxWidth(args.maxWidth());
            // 文本不参与缩放，只裁掉仍在过渡边界之外的部分。
            AbstractGuiUtils.pushScissor(drawX, drawY, drawWidth, drawHeight);
            try {
                LabelWidget.drawLimitedText(clone);
            } finally {
                AbstractGuiUtils.popScissor();
            }
        });
    }

    private static final class PopupRenderData {
        private final String contentKey;
        private final FontDrawArgs args;
        private final BaniraColorConfig theme;
        private final boolean useThemeColor;
        private final TextureUtils.NinePatchInfo ninePatchInfo;
        private final float textureScale;
        private final int paddingLeft;
        private final int paddingRight;
        private final int paddingTop;
        private final int paddingBottom;
        private final int maxWidthForText;
        private final TooltipBounds bounds;

        private PopupRenderData(String contentKey, FontDrawArgs args, BaniraColorConfig theme,
                                boolean useThemeColor, TextureUtils.NinePatchInfo ninePatchInfo,
                                float textureScale, int paddingLeft, int paddingRight,
                                int paddingTop, int paddingBottom, int maxWidthForText,
                                TooltipBounds bounds) {
            this.contentKey = contentKey;
            this.args = args;
            this.theme = theme;
            this.useThemeColor = useThemeColor;
            this.ninePatchInfo = ninePatchInfo;
            this.textureScale = textureScale;
            this.paddingLeft = paddingLeft;
            this.paddingRight = paddingRight;
            this.paddingTop = paddingTop;
            this.paddingBottom = paddingBottom;
            this.maxWidthForText = maxWidthForText;
            this.bounds = bounds;
        }
    }

    public TooltipWidget text(String text) {
        this.text = Text.literal(text);
        tooltip.clear();
        return this;
    }

    public TooltipWidget text(Component component) {
        this.text = Text.from(component);
        tooltip.clear();
        return this;
    }

    public TooltipWidget text(Text text) {
        this.text = text;
        tooltip.clear();
        return this;
    }

    public TooltipWidget itemStack(@Nullable ItemStack itemStack) {
        this.itemStack = itemStack;
        return this;
    }
}
