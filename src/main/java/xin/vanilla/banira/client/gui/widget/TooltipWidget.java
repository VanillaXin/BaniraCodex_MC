package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.Identifier;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumRenderDepth;
import xin.vanilla.banira.client.enums.EnumTooltipTextureMode;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
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
import java.util.List;

import static xin.vanilla.banira.client.data.BaniraColorToken.POPUP_BG;
import static xin.vanilla.banira.client.data.BaniraColorToken.POPUP_BORDER;

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
    public void render(PoseStack stack, float partialTicks) {
        if (!visible) return;
        if (screen instanceof BaniraScreen && screen.isAnyDropdownSelectOpen()) {
            renderChildren(stack, partialTicks);
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
                screen.addDeferredTooltipRender(s -> {
                    s.pushPose();
                    s.last().pose().setIdentity();
                    if (itemStack != null && !itemStack.isEmpty()) {
                        drawItemTooltip(s, itemStack, mouseX, mouseY, seasonTooltip ? screen.season() : null);
                    } else if (vanillaTooltip) {
                        screen.renderComponentTooltip(s, vanillaTooltipLines(), mouseX, mouseY);
                    } else {
                        drawPopupMessage(s, FontDrawArgs.ofPopo(textToDraw.stack(s)).x(mouseX).y(mouseY).popupUseTexture(useTexture), theme, season);
                    }
                    s.popPose();
                });
            } else {
                stack.pushPose();
                if (parent != null) {
                    stack.translate(-absoluteX(), -absoluteY(), 0);
                }
                if (itemStack != null && !itemStack.isEmpty()) {
                    drawItemTooltip(stack, itemStack, mouseX, mouseY, seasonTooltip ? screen.season() : null);
                } else if (vanillaTooltip) {
                    screen.renderComponentTooltip(stack, vanillaTooltipLines(), mouseX, mouseY);
                } else {
                    BaniraColorConfig theme = screen.getEffectiveTheme();
                    EnumSeason season = screen.season();
                    drawPopupMessage(stack, FontDrawArgs.ofPopo(text.stack(stack)).x(mouseX).y(mouseY).popupUseTexture(resolvePopupUseTexture(theme)), theme, season);
                }
                stack.popPose();
            }
        }
        renderChildren(stack, partialTicks);
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
     * 原版 tooltip 的 Component 转换较稳定，文本变化时由 text(...) 统一清空缓存。
     */
    private List<net.minecraft.network.chat.Component> vanillaTooltipLines() {
        if (tooltip.isEmpty()) {
            tooltip.add(text.toComponent().toChat());
        }
        return tooltip;
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

        if (useTextureMode) {
            useTexture(drawArgs, season);
            drawPopupMessageInternal(stack, drawArgs, null);
        } else {
            useColor(drawArgs, theme, season);
            drawPopupMessageInternal(stack, drawArgs, resolveTheme(theme, season));
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
        boolean advanced = BaniraInput.isShiftDown();
        List<Component> tooltipList = ItemUtils.getItemTooltip(itemStack, advanced);
        drawItemTooltipComponents(stack, tooltipList, x, y, season);
    }

    /**
     * 使用调用方已取得的原版 tooltip 行绘制 Banira 样式，避免同一帧重复解析物品提示。
     */
    public static void drawItemTooltipLines(PoseStack stack, List<net.minecraft.network.chat.Component> tooltipList,
                                            double x, double y, @Nullable EnumSeason season) {
        if (tooltipList == null || tooltipList.isEmpty()) {
            return;
        }
        List<Component> converted = new ArrayList<>(tooltipList.size());
        for (net.minecraft.network.chat.Component component : tooltipList) {
            converted.add(BaniraComponent.get().object(component));
        }
        drawItemTooltipComponents(stack, converted, x, y, season);
    }

    private static void drawItemTooltipComponents(PoseStack stack, List<Component> tooltipList,
                                                  double x, double y, @Nullable EnumSeason season) {
        if (tooltipList == null || tooltipList.isEmpty()) {
            return;
        }
        Component tooltipComponent = BaniraComponent.get().empty();
        for (int idx = 0; idx < tooltipList.size(); idx++) {
            Component component = tooltipList.get(idx);
            if (idx > 0) tooltipComponent = tooltipComponent.append("\n");
            tooltipComponent = tooltipComponent.append(component);
        }
        Text tooltipText = new Text(tooltipComponent);
        Font font = AbstractGuiUtils.getFont();
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

    private static void drawPopupMessageInternal(PoseStack stack, FontDrawArgs args, @Nullable BaniraColorConfig theme) {
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
        }

        FontDrawArgs calcArgs = args.clone()
                .paddingLeft(calculatedPaddingLeft).paddingRight(calculatedPaddingRight)
                .paddingTop(calculatedPaddingTop).paddingBottom(calculatedPaddingBottom);
        KeyValue<Integer, Integer> textSize = LabelWidget.calculateLimitedTextSize(calcArgs);
        int textWidth = textSize.key();
        int textHeight = textSize.val();

        final int finalCalculatedPaddingLeft = calculatedPaddingLeft;
        final int finalCalculatedPaddingRight = calculatedPaddingRight;
        final int finalCalculatedPaddingTop = calculatedPaddingTop;
        final int finalCalculatedPaddingBottom = calculatedPaddingBottom;
        final float textureScale = calculatedTextureScale;

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

            adjustedX = args.x() - msgWidth / 2.0;
            adjustedY = args.y() - msgHeight - 5;

            boolean hasTopSpace = adjustedY >= args.marginTop();
            boolean hasLeftSpace = adjustedX >= args.marginLeft();
            boolean hasRightSpace = adjustedX + msgWidth <= screenWidth - args.marginRight();

            if (!hasTopSpace) {
                adjustedY = args.y() + 1 + 5;
            } else {
                if (!hasLeftSpace) adjustedX = args.marginLeft();
                else if (!hasRightSpace) adjustedX = screenWidth - msgWidth - args.marginRight();
            }

            adjustedX = Math.max(args.marginLeft(), Math.min(adjustedX, screenWidth - msgWidth - args.marginRight()));
            adjustedY = Math.max(args.marginTop(), Math.min(adjustedY, screenHeight - msgHeight - args.marginBottom()));

            if (args.wrap()) {
                int actualAvailableWidth = screenWidth - (int) adjustedX - args.marginRight();
                if (finalMaxWidth > 0) actualAvailableWidth = Math.min(actualAvailableWidth, finalMaxWidth);
                actualAvailableWidth = Math.max(actualAvailableWidth, finalCalculatedPaddingLeft + finalCalculatedPaddingRight);
                finalMaxWidth = actualAvailableWidth;
            }
        }

        final double finalAdjustedX = adjustedX;
        final double finalAdjustedY = adjustedY;
        final int finalMsgWidth = msgWidth;
        final int finalMsgHeight = msgHeight;
        final int finalMaxWidthForText = finalMaxWidth;

        AbstractGuiUtils.renderByDepth(args.text().stack(), EnumRenderDepth.TOOLTIP, (s) -> {
            if (args.texture() != null && ninePatchInfo != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                NinePatchImageWidget.drawNinePatch(s, args.texture(), (int) finalAdjustedX, (int) finalAdjustedY, finalMsgWidth, finalMsgHeight, textureScale);
                AbstractGuiUtils.restoreGuiRenderState();
            } else if (args.texture() != null) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                Texture tex = args.texture();
                ImageWidget.blit(s, tex, (int) finalAdjustedX, (int) finalAdjustedY, finalMsgWidth, finalMsgHeight);
                AbstractGuiUtils.restoreGuiRenderState();
            } else if (useThemeColor && theme != null) {
                int radius = args.bgBorderRadius();
                int borderThickness = args.bgBorderThickness();
                ShapeDrawArgs.RoundedCornerMode cornerMode = args.popupCornerMode() != null ? args.popupCornerMode() : ShapeDrawArgs.RoundedCornerMode.FINE;
                ShapeDrawArgs fillArgs = ShapeDrawArgs.rect(s, (float) finalAdjustedX, (float) finalAdjustedY, finalMsgWidth, finalMsgHeight, theme.color(POPUP_BG));
                fillArgs.rect().radius(radius).cornerMode(cornerMode);
                BaseShapeWidget.drawShape(fillArgs);
                if (borderThickness > 0) {
                    ShapeDrawArgs borderArgs = ShapeDrawArgs.rect(s, (float) finalAdjustedX, (float) finalAdjustedY, finalMsgWidth, finalMsgHeight, theme.color(POPUP_BORDER));
                    borderArgs.rect().radius(radius).border(borderThickness).cornerMode(cornerMode);
                    BaseShapeWidget.drawShape(borderArgs);
                }
            } else {
                int borderRadius = args.bgBorderRadius();
                int borderThickness = args.bgBorderThickness();
                AbstractGuiUtils.drawRoundedRect(args.text().stack(), (int) finalAdjustedX, (int) finalAdjustedY, finalMsgWidth, finalMsgHeight, args.bgArgb(), borderRadius);
                int borderArgb = ColorUtils.softenArgb(args.bgArgb());
                AbstractGuiUtils.drawRoundedRectOutLineRough(args.text().stack(), (int) finalAdjustedX, (int) finalAdjustedY, finalMsgWidth, finalMsgHeight, borderThickness, borderArgb, borderRadius);
            }

            FontDrawArgs clone = args.clone()
                    .x(finalAdjustedX).y(finalAdjustedY)
                    .bgArgb(0x00000000).position(EnumEllipsisPosition.MIDDLE)
                    .paddingLeft(finalCalculatedPaddingLeft).paddingRight(finalCalculatedPaddingRight)
                    .paddingTop(finalCalculatedPaddingTop).paddingBottom(finalCalculatedPaddingBottom);
            if (args.wrap() && finalMaxWidthForText > 0) clone.maxWidth(finalMaxWidthForText);
            else if (args.maxWidth() > 0) clone.maxWidth(args.maxWidth());
            LabelWidget.drawLimitedText(clone);
        });
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
