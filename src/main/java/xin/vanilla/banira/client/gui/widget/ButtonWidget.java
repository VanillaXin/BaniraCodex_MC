package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.FontRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.PressGestureState;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 按钮Widget
 */
@Accessors(chain = true, fluent = true)
public class ButtonWidget extends BaseWidget implements ITextWidget {

    /**
     * 预置图标样式
     */
    public enum PresetStyle {
        /**
         * 红叉关闭
         */
        CLOSE,
        /**
         * 减号/最小化
         */
        MINUS,
        /**
         * 加号
         */
        PLUS,
        /**
         * 最大化方框
         */
        MAXIMIZE,
        /**
         * 上箭头
         */
        ARROW_UP,
        /**
         * 下箭头
         */
        ARROW_DOWN,
        /**
         * 左箭头
         */
        ARROW_LEFT,
        /**
         * 右箭头
         */
        ARROW_RIGHT,
        /**
         * 重置/恢复（圆形箭头）
         */
        RESET,
    }

    @FunctionalInterface
    public interface LeadingIconRenderer {
        void render(MatrixStack stack, int x, int y, int size);
    }

    /**
     * 长按进度条填充方向
     */
    public enum LongPressProgressMode {
        /**
         * 从左至右，粒子向右崩裂并下落
         */
        LEFT_TO_RIGHT,
        /**
         * 从右至左，粒子向左崩裂并下落
         */
        RIGHT_TO_LEFT,
        /**
         * 由内向外扩展，粒子从四周边缘崩出并下落
         */
        INSIDE_OUT,
        /**
         * 由外向内收缩，粒子在中心崩出并下落
         */
        OUTSIDE_IN,
        /**
         * 从上至下，粒子从下边崩出并下落
         */
        TOP_TO_BOTTOM,
        /**
         * 从下至上，粒子向上崩出并下落
         */
        BOTTOM_TO_TOP,
        ;
    }

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 长按完成反馈最长显示时间（毫秒）。
     */
    private static final int LONG_PRESS_COMPLETION_DURATION_MS = 720;

    private static final int COMPLETION_KIND_FLASH = 0;
    private static final int COMPLETION_KIND_CHECK = 1;

    private final List<LongPressCompletionEffect> longPressCompletionEffects = new ArrayList<>();

    /**
     * 长按进度条模式，默认从左至右
     */
    @Getter
    @Setter
    private LongPressProgressMode longPressProgressMode = LongPressProgressMode.INSIDE_OUT;

    @Getter
    private Text text = Text.empty();

    @Getter
    @Setter
    private Consumer<ButtonWidget> onClick;

    /**
     * 左键长按达到 {@link #longPressDurationMs} 时触发；与 {@link #onClick} 互斥（长按触发后释放不再触发单击）。
     */
    @Getter
    private Consumer<ButtonWidget> longPressHandler;

    /**
     * 长按判定时间（毫秒），默认 2000。
     */
    @Getter
    private long longPressDurationMs = 2000L;

    private final PressGestureState longPressState = new PressGestureState();

    @Getter
    @Setter
    private int bgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBg();

    @Getter
    @Setter
    private int hoverBgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBgHover();

    @Getter
    @Setter
    private int focusedBgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBgFocused();

    @Getter
    @Setter
    private int pressedBgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBgPressed();

    @Getter
    @Setter
    private int disabledBgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBgDisabled();

    @Getter
    @Setter
    private int borderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBorder();

    @Getter
    @Setter
    private int hoverBorderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBorderHover();

    @Getter
    @Setter
    private int focusedBorderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBorderFocused();

    @Getter
    @Setter
    private int pressedBorderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBorderPressed();

    @Getter
    @Setter
    private int disabledBorderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonBorderDisabled();

    @Getter
    @Setter
    private int borderWidth = 1;

    @Getter
    @Setter
    private int textColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonText();

    @Getter
    @Setter
    private int hoverTextColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonTextHover();

    @Getter
    @Setter
    private int focusedTextColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonTextFocused();

    @Getter
    @Setter
    private int pressedTextColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonTextPressed();

    @Getter
    @Setter
    private int disabledTextColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonTextDisabled();

    /**
     * 长按进度填充色（与 hover 轨道区分），由主题 {@link BaniraColorConfig#buttonLongPressProgressFill()} 提供
     */
    @Getter
    @Setter
    private int longPressProgressFillColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).buttonLongPressProgressFill();

    @Getter
    @Setter
    private float fontSize = 9.0f;

    @Getter
    @Setter
    private int paddingLeft = 4;

    @Getter
    @Setter
    private int paddingRight = 4;

    @Getter
    @Setter
    private int paddingTop = 4;

    @Getter
    @Setter
    private int paddingBottom = 4;

    @Getter
    @Setter
    private int marginLeft = 0;

    @Getter
    @Setter
    private int marginRight = 0;

    @Getter
    @Setter
    private int marginTop = 0;

    @Getter
    @Setter
    private int marginBottom = 0;

    /**
     * 文本最大宽度，>0 时超出部分以省略号表示
     */
    @Getter
    @Setter
    private int textMaxWidth = 0;

    /**
     * 省略号位置，textMaxWidth>0 时生效
     */
    @Getter
    @Setter
    private EnumEllipsisPosition textEllipsisPosition = EnumEllipsisPosition.END;

    /**
     * 预置图标样式，非 null 时绘制图标而非文本
     */
    @Getter
    private PresetStyle presetStyle = null;

    /** 可选的内容图标，与文本一起居中绘制。 */
    @Getter
    @Setter
    @Nullable
    private LeadingIconRenderer leadingIconRenderer;

    @Getter
    @Setter
    private int leadingIconSize = 14;

    /**
     * 图标颜色（presetStyle 非 null 时生效）
     */
    @Getter
    @Setter
    private int iconColor = 0xFF333333;

    @Getter
    @Setter
    private int hoverIconColor = 0xFF555555;

    @Getter
    @Setter
    private int focusedIconColor = 0xFF444444;

    @Getter
    @Setter
    private int pressedIconColor = 0xFF222222;

    @Getter
    @Setter
    private int disabledIconColor = 0xFFAAAAAA;

    /**
     * 图标线条宽度
     */
    @Getter
    @Setter
    private float iconStrokeWidth = 1.5f;

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        bgColor(theme.buttonBg()).hoverBgColor(theme.buttonBgHover())
                .focusedBgColor(theme.buttonBgFocused()).pressedBgColor(theme.buttonBgPressed())
                .disabledBgColor(theme.buttonBgDisabled())
                .borderColor(theme.buttonBorder()).hoverBorderColor(theme.buttonBorderHover())
                .focusedBorderColor(theme.buttonBorderFocused()).pressedBorderColor(theme.buttonBorderPressed())
                .disabledBorderColor(theme.buttonBorderDisabled())
                .textColor(theme.buttonText()).hoverTextColor(theme.buttonTextHover())
                .focusedTextColor(theme.buttonTextFocused()).pressedTextColor(theme.buttonTextPressed())
                .disabledTextColor(theme.buttonTextDisabled())
                .longPressProgressFillColor(theme.buttonLongPressProgressFill());
        if (presetStyle != null && presetStyle != PresetStyle.CLOSE) {
            iconColor(theme.buttonPresetIconColor())
                    .hoverIconColor(theme.buttonPresetIconHoverColor())
                    .focusedIconColor(theme.buttonPresetIconFocusedColor())
                    .pressedIconColor(theme.buttonPresetIconPressedColor())
                    .disabledIconColor(theme.buttonPresetIconDisabledColor());
        }
    }

    /**
     * 左上圆角半径
     */
    @Getter
    @Setter
    private float topLeftRadius = 2.0f;

    /**
     * 右上圆角半径
     */
    @Getter
    @Setter
    private float topRightRadius = 2.0f;

    /**
     * 左下圆角半径
     */
    @Getter
    @Setter
    private float bottomLeftRadius = 2.0f;

    /**
     * 右下圆角半径
     */
    @Getter
    @Setter
    private float bottomRightRadius = 2.0f;

    @Getter
    @Setter
    private ShapeDrawArgs.RoundedCornerMode cornerMode = ShapeDrawArgs.RoundedCornerMode.FINE;

    /**
     * 四角使用同一圆角半径
     */
    public ButtonWidget radius(float radius) {
        this.topLeftRadius = radius;
        this.topRightRadius = radius;
        this.bottomLeftRadius = radius;
        this.bottomRightRadius = radius;
        return this;
    }

    /**
     * 分别指定左上、右上、左下、右下圆角半径
     */
    public ButtonWidget radius(float topLeft, float topRight, float bottomLeft, float bottomRight) {
        this.topLeftRadius = topLeft;
        this.topRightRadius = topRight;
        this.bottomLeftRadius = bottomLeft;
        this.bottomRightRadius = bottomRight;
        return this;
    }

    /**
     * 四角圆角一致时返回该值，否则返回四角中的最大值
     */
    public float radius() {
        if (topLeftRadius == topRightRadius && topLeftRadius == bottomLeftRadius && topLeftRadius == bottomRightRadius) {
            return topLeftRadius;
        }
        return Math.max(Math.max(topLeftRadius, topRightRadius), Math.max(bottomLeftRadius, bottomRightRadius));
    }

    // region 圆角绘制

    private void applyButtonRectCorners(ShapeDrawArgs shape) {
        shape.rect().radius(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius).cornerMode(cornerMode);
    }

    // endregion 圆角绘制

    public ButtonWidget(BaniraScreen screen) {
        super(screen);
    }

    public ButtonWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
    }

    public ButtonWidget(BaniraScreen screen, ScreenCoordinate bounds, Component text) {
        super(screen, bounds);
        this.text = Text.from(text);
    }

    public ButtonWidget padding(int padding) {
        paddingLeft(padding);
        paddingRight(padding);
        paddingTop(padding);
        paddingBottom(padding);
        return this;
    }

    /**
     * 红叉关闭按钮预设
     */
    public ButtonWidget presetStyleClose() {
        presetStyle(PresetStyle.CLOSE);
        iconColor(0xFFE53935);
        hoverIconColor(0xFFFF5252);
        pressedIconColor(0xFFC62828);
        focusedIconColor(0xFFEF5350);
        disabledIconColor(0xFFB0BEC5);
        iconStrokeWidth(2f);
        borderWidth(0);
        return this;
    }

    /**
     * 预置样式快捷设置
     */
    public ButtonWidget presetStyle(PresetStyle style) {
        this.presetStyle = style;
        return this;
    }

    public ButtonWidget onLongPress(Consumer<ButtonWidget> handler) {
        return onLongPress(2000L, handler);
    }

    public ButtonWidget onLongPress(long durationMs, Consumer<ButtonWidget> handler) {
        this.longPressDurationMs = Math.max(1L, durationMs);
        this.longPressHandler = handler;
        return this;
    }

    @Override
    protected long genericLongPressThresholdMs() {
        return longPressHandler != null ? Long.MAX_VALUE : super.genericLongPressThresholdMs();
    }

    @Override
    public void render(MatrixStack stack, float partialTicks) {
        if (!visible) {
            return;
        }

        if (renderCoordinate == null) {
            return;
        }

        int x = (int) renderCoordinate.x();
        int y = (int) renderCoordinate.y();
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();

        int drawX = x + marginLeft;
        int drawY = y + marginTop;
        int drawWidth = width - marginLeft - marginRight;
        int drawHeight = height - marginTop - marginBottom;

        boolean longPressProgress = enabled && longPressHandler != null && longPressState.pressing(0);

        int currentBgColor;
        int currentBorderColor;
        int currentTextColor;
        int currentIconColor;

        if (longPressProgress) {
            float progress = longPressState.progress(longPressDurationMs);
            int absClipX = (int) Math.round(absoluteX()) + marginLeft;
            int absClipY = (int) Math.round(absoluteY()) + marginTop;

            ShapeDrawArgs track = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, hoverBgColor);
            applyButtonRectCorners(track);
            BaseShapeWidget.drawShape(track);

            renderLongPressPressedFill(stack, drawX, drawY, drawWidth, drawHeight, progress, absClipX, absClipY);

            if (borderWidth > 0) {
                currentBorderColor = hoverBorderColor;
                ShapeDrawArgs border = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, currentBorderColor);
                applyButtonRectCorners(border);
                border.rect().border(borderWidth);
                BaseShapeWidget.drawShape(border);
            }

            currentTextColor = hoverTextColor;
            currentIconColor = hoverIconColor;
        } else {
            if (!enabled) {
                currentBgColor = disabledBgColor;
            } else if (mousePressed) {
                currentBgColor = pressedBgColor;
            } else if (mouseInside) {
                currentBgColor = hoverBgColor;
            } else if (focused) {
                currentBgColor = focusedBgColor;
            } else {
                currentBgColor = bgColor;
            }

            ShapeDrawArgs rect = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, currentBgColor);
            applyButtonRectCorners(rect);
            BaseShapeWidget.drawShape(rect);

            if (borderWidth > 0) {
                if (!enabled) {
                    currentBorderColor = disabledBorderColor;
                } else if (mousePressed) {
                    currentBorderColor = pressedBorderColor;
                } else if (mouseInside) {
                    currentBorderColor = hoverBorderColor;
                } else if (focused) {
                    currentBorderColor = focusedBorderColor;
                } else {
                    currentBorderColor = borderColor;
                }

                ShapeDrawArgs border = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, currentBorderColor);
                applyButtonRectCorners(border);
                border.rect().border(borderWidth);
                BaseShapeWidget.drawShape(border);
            }

            if (!enabled) {
                currentTextColor = disabledTextColor;
            } else if (mousePressed) {
                currentTextColor = pressedTextColor;
            } else if (mouseInside) {
                currentTextColor = hoverTextColor;
            } else if (focused) {
                currentTextColor = focusedTextColor;
            } else {
                currentTextColor = textColor;
            }

            if (!enabled) {
                currentIconColor = disabledIconColor;
            } else if (mousePressed) {
                currentIconColor = pressedIconColor;
            } else if (mouseInside) {
                currentIconColor = hoverIconColor;
            } else if (focused) {
                currentIconColor = focusedIconColor;
            } else {
                currentIconColor = iconColor;
            }
        }

        FontRenderer font = AbstractGuiUtils.getFont();

        int contentX = drawX + paddingLeft;
        int contentY = drawY + paddingTop;
        int availableWidth = drawWidth - paddingLeft - paddingRight;
        int availableHeight = drawHeight - paddingTop - paddingBottom;

        // 预置图标或文本
        if (presetStyle != null) {
            drawPresetIcon(stack, contentX, contentY, availableWidth, availableHeight, currentIconColor);
        } else if (leadingIconRenderer != null) {
            int iconSize = Math.max(1, Math.min(leadingIconSize, availableHeight));
            int gap = 4;
            int maxTextWidth = Math.max(0, availableWidth - iconSize - gap);
            int naturalTextWidth = AbstractGuiUtils.getTextWidth(font, this.text());
            int renderedTextWidth = Math.min(naturalTextWidth, maxTextWidth);
            int totalWidth = iconSize + (renderedTextWidth > 0 ? gap + renderedTextWidth : 0);
            int startX = contentX + Math.max(0, (availableWidth - totalWidth) / 2);
            leadingIconRenderer.render(stack, startX, contentY + (availableHeight - iconSize) / 2, iconSize);
            if (renderedTextWidth > 0) {
                FontDrawArgs drawArgs = FontDrawArgs.of(text.stack(stack).color(currentTextColor)).inScreen(false);
                LabelWidget.drawLimitedText(drawArgs.x(startX + iconSize + gap)
                        .y(contentY + (availableHeight - 9) / 2)
                        .maxWidth(maxTextWidth).position(EnumEllipsisPosition.END));
            }
        } else {
            FontDrawArgs drawArgs = FontDrawArgs.of(text.stack(stack).color(currentTextColor)).inScreen(false);
            if (textMaxWidth > 0 && textEllipsisPosition != EnumEllipsisPosition.NONE) {
                LabelWidget.drawLimitedText(drawArgs.x(contentX).y(contentY + (availableHeight - 9) / 2)
                        .maxWidth(Math.min(textMaxWidth, availableWidth))
                        .position(textEllipsisPosition));
            } else if (fontSize != 9.0f) {
                stack.pushPose();
                stack.translate(contentX, contentY, 0);
                float scale = fontSize / 9.0f;
                stack.scale(scale, scale, 1.0f);
                int textWidth = AbstractGuiUtils.getTextWidth(font, this.text());
                int textHeight = 9;
                int scaledTextX = (int) ((availableWidth / scale - textWidth) / 2.0);
                int scaledTextY = (int) ((availableHeight / scale - textHeight) / 2.0);
                LabelWidget.drawLimitedText(drawArgs.x(scaledTextX).y(scaledTextY));
                stack.popPose();
            } else {
                int textWidth = AbstractGuiUtils.getTextWidth(font, this.text());
                int textHeight = 9;
                int centeredTextX = contentX + (availableWidth - textWidth) / 2;
                int centeredTextY = contentY + (availableHeight - textHeight) / 2;
                LabelWidget.drawLimitedText(drawArgs.x(centeredTextX).y(centeredTextY));
            }
        }

        if (!longPressCompletionEffects.isEmpty()) {
            drawLongPressCompletionEffects(stack, drawX, drawY, drawWidth, drawHeight);
        }

        renderChildren(stack, partialTicks);
    }

    private void renderLongPressPressedFill(MatrixStack stack, int drawX, int drawY, int drawWidth, int drawHeight, float progress, int absClipX, int absClipY) {
        switch (longPressProgressMode) {
            case LEFT_TO_RIGHT:
                renderLongPressFillSingleScissor(stack, drawX, drawY, drawWidth, drawHeight, absClipX, absClipY,
                        Math.max(0, Math.min(drawWidth, (int) Math.ceil(drawWidth * progress))), drawHeight, 0, 0);
                break;
            case RIGHT_TO_LEFT: {
                int fillW = Math.max(0, Math.min(drawWidth, (int) Math.ceil(drawWidth * progress)));
                renderLongPressFillSingleScissor(stack, drawX, drawY, drawWidth, drawHeight, absClipX, absClipY,
                        fillW, drawHeight, drawWidth - fillW, 0);
                break;
            }
            case TOP_TO_BOTTOM: {
                int fillH = Math.max(0, Math.min(drawHeight, (int) Math.ceil(drawHeight * progress)));
                renderLongPressFillSingleScissor(stack, drawX, drawY, drawWidth, drawHeight, absClipX, absClipY,
                        drawWidth, fillH, 0, 0);
                break;
            }
            case BOTTOM_TO_TOP: {
                int fillH = Math.max(0, Math.min(drawHeight, (int) Math.ceil(drawHeight * progress)));
                renderLongPressFillSingleScissor(stack, drawX, drawY, drawWidth, drawHeight, absClipX, absClipY,
                        drawWidth, fillH, 0, drawHeight - fillH);
                break;
            }
            case INSIDE_OUT: {
                int fw = Math.max(0, Math.min(drawWidth, (int) Math.ceil(drawWidth * progress)));
                int fh = Math.max(0, Math.min(drawHeight, (int) Math.ceil(drawHeight * progress)));
                if (fw > 0 && fh > 0) {
                    int ox = (drawWidth - fw) / 2;
                    int oy = (drawHeight - fh) / 2;
                    renderLongPressFillSingleScissor(stack, drawX, drawY, drawWidth, drawHeight, absClipX, absClipY,
                            fw, fh, ox, oy);
                }
                break;
            }
            case OUTSIDE_IN:
                renderLongPressOutsideInFill(stack, drawX, drawY, drawWidth, drawHeight, progress, absClipX, absClipY);
                break;
        }
    }

    private void renderLongPressFillSingleScissor(MatrixStack stack, int drawX, int drawY, int drawWidth, int drawHeight, int absClipX, int absClipY,
                                                  int clipW, int clipH, int clipOffsetX, int clipOffsetY) {
        if (clipW <= 0 || clipH <= 0) {
            return;
        }
        AbstractGuiUtils.pushScissor(absClipX + clipOffsetX, absClipY + clipOffsetY, clipW, clipH);
        ShapeDrawArgs fill = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, longPressProgressFillColor);
        applyButtonRectCorners(fill);
        BaseShapeWidget.drawShape(fill);
        AbstractGuiUtils.popScissor();
    }

    private void renderLongPressOutsideInFill(MatrixStack stack, int drawX, int drawY, int drawWidth, int drawHeight, float progress, int absClipX, int absClipY) {
        int innerW = (int) Math.floor(drawWidth * (1f - progress));
        int innerH = (int) Math.floor(drawHeight * (1f - progress));
        innerW = Math.max(0, Math.min(drawWidth, innerW));
        innerH = Math.max(0, Math.min(drawHeight, innerH));
        int holeX = absClipX + (drawWidth - innerW) / 2;
        int holeY = absClipY + (drawHeight - innerH) / 2;

        int topH = holeY - absClipY;
        if (topH > 0) {
            AbstractGuiUtils.pushScissor(absClipX, absClipY, drawWidth, topH);
            ShapeDrawArgs fill = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, longPressProgressFillColor);
            applyButtonRectCorners(fill);
            BaseShapeWidget.drawShape(fill);
            AbstractGuiUtils.popScissor();
        }

        int bottomY = holeY + innerH;
        int bottomH = absClipY + drawHeight - bottomY;
        if (bottomH > 0) {
            AbstractGuiUtils.pushScissor(absClipX, bottomY, drawWidth, bottomH);
            ShapeDrawArgs fill = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, longPressProgressFillColor);
            applyButtonRectCorners(fill);
            BaseShapeWidget.drawShape(fill);
            AbstractGuiUtils.popScissor();
        }

        int leftW = holeX - absClipX;
        if (leftW > 0 && innerH > 0) {
            AbstractGuiUtils.pushScissor(absClipX, holeY, leftW, innerH);
            ShapeDrawArgs fill = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, longPressProgressFillColor);
            applyButtonRectCorners(fill);
            BaseShapeWidget.drawShape(fill);
            AbstractGuiUtils.popScissor();
        }

        int rightX = holeX + innerW;
        int rightW = absClipX + drawWidth - rightX;
        if (rightW > 0 && innerH > 0) {
            AbstractGuiUtils.pushScissor(rightX, holeY, rightW, innerH);
            ShapeDrawArgs fill = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, longPressProgressFillColor);
            applyButtonRectCorners(fill);
            BaseShapeWidget.drawShape(fill);
            AbstractGuiUtils.popScissor();
        }
    }

    // region 长按完成反馈

    private void tickLongPressCompletionEffects() {
        long now = System.currentTimeMillis();
        longPressCompletionEffects.removeIf(e -> now - e.spawnTimeMs > e.lifetimeMs);
    }

    private void spawnLongPressCompletionFeedback(int drawWidth, int drawHeight) {
        longPressCompletionEffects.clear();
        if (drawWidth < 1 || drawHeight < 1) {
            return;
        }
        long now = System.currentTimeMillis();
        LongPressCompletionPalette colors = longPressCompletionPalette();
        longPressCompletionEffects.add(new LongPressCompletionEffect(colors.wave, now,
                drawWidth, drawHeight, COMPLETION_KIND_FLASH, 560));
        float markSize = Math.max(8f, Math.min(18f, Math.min(drawWidth, drawHeight) * 0.62f));
        longPressCompletionEffects.add(new LongPressCompletionEffect(colors.sparkAccent, now,
                markSize, markSize, COMPLETION_KIND_CHECK, LONG_PRESS_COMPLETION_DURATION_MS));
    }

    private LongPressCompletionPalette longPressCompletionPalette() {
        int base = opaqueArgb(longPressProgressFillColor);
        int hover = opaqueArgb(hoverBgColor);
        int focus = opaqueArgb(focusedBorderColor != 0 ? focusedBorderColor : hoverBorderColor);
        int sparkAccent = brightenArgb(blendArgb(base, focus, 0.42f), 1.16f);
        int wave = brightenArgb(blendArgb(base, hover, 0.24f), 1.10f);
        return new LongPressCompletionPalette(sparkAccent, wave);
    }

    private static int opaqueArgb(int argb) {
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }

    private static int brightenArgb(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int cr = (argb >> 16) & 0xFF;
        int cg = (argb >> 8) & 0xFF;
        int cb = argb & 0xFF;
        cr = Math.min(255, Math.round(cr * factor));
        cg = Math.min(255, Math.round(cg * factor));
        cb = Math.min(255, Math.round(cb * factor));
        return (a << 24) | (cr << 16) | (cg << 8) | cb;
    }

    private static int blendArgb(int from, int to, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        int a = Math.round(((from >>> 24) & 0xFF) * (1f - clamped)
                + ((to >>> 24) & 0xFF) * clamped);
        int r = Math.round(((from >> 16) & 0xFF) * (1f - clamped)
                + ((to >> 16) & 0xFF) * clamped);
        int g = Math.round(((from >> 8) & 0xFF) * (1f - clamped)
                + ((to >> 8) & 0xFF) * clamped);
        int b = Math.round((from & 0xFF) * (1f - clamped)
                + (to & 0xFF) * clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawLongPressCompletionEffects(MatrixStack stack, int drawX, int drawY,
                                                int drawWidth, int drawHeight) {
        if (drawWidth < 1 || drawHeight < 1) {
            return;
        }
        float centerX = drawX + drawWidth * 0.5f;
        float centerY = drawY + drawHeight * 0.5f;
        long now = System.currentTimeMillis();
        for (LongPressCompletionEffect effect : longPressCompletionEffects) {
            float elapsedMs = now - effect.spawnTimeMs;
            if (elapsedMs <= 0f || elapsedMs >= effect.lifetimeMs) {
                continue;
            }
            float age = elapsedMs / effect.lifetimeMs;
            if (effect.kind == COMPLETION_KIND_FLASH) {
                drawLongPressCompletionFlash(stack, centerX, centerY, effect, elapsedMs, age);
            } else if (effect.kind == COMPLETION_KIND_CHECK) {
                drawLongPressCompletionCheck(stack, centerX, centerY, effect, age);
            }
        }
    }

    /**
     * 完成时短暂提亮按钮，并用扫光连接进度态与完成态。
     */
    private void drawLongPressCompletionFlash(MatrixStack stack, float centerX, float centerY,
                                              LongPressCompletionEffect effect,
                                              float elapsedMs, float age) {
        int width = Math.max(1, Math.round(effect.drawW));
        int height = Math.max(1, Math.round(effect.drawH));
        int x = Math.round(centerX - width * 0.5f);
        int y = Math.round(centerY - height * 0.5f);
        float fade = 1f - age;
        int washAlpha = (int) (72f * fade * fade);
        if (washAlpha > 5) {
            ShapeDrawArgs wash = ShapeDrawArgs.rect(stack, x, y, width, height,
                    withAlphaArgb(effect.baseColor, washAlpha));
            applyButtonRectCorners(wash);
            BaseShapeWidget.drawShape(wash);
        }

        float sweepProgress = Math.min(1f, elapsedMs / 360f);
        int sweepAlpha = (int) (110f * (1f - Math.abs(sweepProgress - 0.5f) * 1.45f)
                * Math.max(0f, fade));
        if (sweepAlpha > 5) {
            int sweepWidth = Math.max(5, Math.round(width * 0.24f));
            int sweepX = x - sweepWidth + Math.round((width + sweepWidth * 2f) * sweepProgress);
            AbstractGuiUtils.pushScissor(x, y, width, height);
            AbstractGuiUtils.fill(stack, sweepX, y, sweepWidth, height,
                    withAlphaArgb(effect.baseColor, sweepAlpha));
            AbstractGuiUtils.fill(stack, sweepX + sweepWidth / 2, y, 1, height,
                    withAlphaArgb(brightenArgb(effect.baseColor, 1.18f),
                            Math.min(255, sweepAlpha + 42)));
            AbstractGuiUtils.popScissor();
        }
    }

    /**
     * 对勾明确表示长按动作已经完成。
     */
    private void drawLongPressCompletionCheck(MatrixStack stack, float centerX, float centerY,
                                              LongPressCompletionEffect effect, float age) {
        float pop = age < 0.22f ? age / 0.22f : 1f;
        pop = 1f - (1f - pop) * (1f - pop);
        float fade = age < 0.76f ? 1f : Math.max(0f, (1f - age) / 0.24f);
        int alpha = (int) (235f * fade);
        if (alpha < 8) {
            return;
        }
        float size = effect.drawW * (0.76f + 0.24f * pop);
        float x1 = centerX - size * 0.44f;
        float y1 = centerY + size * 0.02f;
        float x2 = centerX - size * 0.12f;
        float y2 = centerY + size * 0.34f;
        float x3 = centerX + size * 0.48f;
        float y3 = centerY - size * 0.36f;
        int shadow = withAlphaArgb(0xFF000000, (int) (58f * fade));
        int color = withAlphaArgb(effect.baseColor, alpha);
        AbstractGuiUtils.drawLineWithSquareCaps(stack,
                x1 + 0.8f, y1 + 0.8f, x2 + 0.8f, y2 + 0.8f, 3.2f, shadow);
        AbstractGuiUtils.drawLineWithSquareCaps(stack,
                x2 + 0.8f, y2 + 0.8f, x3 + 0.8f, y3 + 0.8f, 3.2f, shadow);
        AbstractGuiUtils.drawLineWithSquareCaps(stack, x1, y1, x2, y2, 2.6f, color);
        AbstractGuiUtils.drawLineWithSquareCaps(stack, x2, y2, x3, y3, 2.6f, color);
    }

    private static int withAlphaArgb(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static final class LongPressCompletionPalette {
        final int sparkAccent;
        final int wave;

        LongPressCompletionPalette(int sparkAccent, int wave) {
            this.sparkAccent = sparkAccent;
            this.wave = wave;
        }
    }

    private static final class LongPressCompletionEffect {
        final int baseColor;
        final long spawnTimeMs;
        final float drawW;
        final float drawH;
        final int kind;
        final int lifetimeMs;

        LongPressCompletionEffect(int baseColor, long spawnTimeMs,
                                  float drawW, float drawH, int kind, int lifetimeMs) {
            this.baseColor = baseColor;
            this.spawnTimeMs = spawnTimeMs;
            this.drawW = drawW;
            this.drawH = drawH;
            this.kind = kind;
            this.lifetimeMs = lifetimeMs;
        }
    }

    // endregion 长按完成反馈

    /**
     * 根据 presetStyle 绘制预置图标（关闭叉、加减号、箭头等）
     */
    private void drawPresetIcon(MatrixStack stack, int x, int y, int w, int h, int color) {
        float iw = Math.max(0f, (float) w);
        float ih = Math.max(0f, (float) h);
        float size = Math.min(iw, ih);
        if (size < 2f) {
            return;
        }
        float cx = x + iw * 0.5f;
        float cy = y + ih * 0.5f;
        float r = Math.max(1f, size * 0.38f);
        if (r > size * 0.42f) {
            r = size * 0.42f;
        }
        float lw = iconStrokeWidth;

        switch (presetStyle) {
            case CLOSE:
                // 叉号
                AbstractGuiUtils.drawLine(stack, cx - r, cy - r, cx + r, cy + r, lw, color);
                AbstractGuiUtils.drawLine(stack, cx + r, cy - r, cx - r, cy + r, lw, color);
                break;
            case MINUS:
                // 减号
                AbstractGuiUtils.drawLine(stack, cx - r, cy, cx + r, cy, lw, color);
                break;
            case PLUS:
                // 加号
                float plusR = Math.max(0.8f, r * 0.88f);
                if (plusR + lw * 0.5f > size * 0.48f) {
                    plusR = Math.max(0.6f, size * 0.48f - lw * 0.5f);
                }
                AbstractGuiUtils.drawLine(stack, cx - plusR, cy, cx + plusR, cy, lw, color);
                AbstractGuiUtils.drawLine(stack, cx, cy - plusR, cx, cy + plusR, lw, color);
                break;
            case MAXIMIZE:
                // 最大化
                ShapeDrawArgs.PolygonParams sq = new ShapeDrawArgs.PolygonParams()
                        .centerX(cx).centerY(cy).radius(r * 0.95f).sides(4).rotation(45).border(lw);
                AbstractGuiUtils.drawPolygonBorder(stack, sq, color);
                break;
            case ARROW_UP:
                // 上箭头
                AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, -90, color);
                break;
            case ARROW_DOWN:
                AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 90, color);
                break;
            case ARROW_LEFT:
                AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 180, color);
                break;
            case ARROW_RIGHT:
                AbstractGuiUtils.drawPolygon(stack, cx, cy, r, 3, 0, color);
                break;
            case RESET:
                drawResetIcon(stack, cx, cy, r, lw, color);
                break;
            default:
                break;
        }
    }

    /**
     * 绘制重置图标
     */
    private void drawResetIcon(MatrixStack stack, float cx, float cy, float r, float lw, int color) {
        float d = r * 1.375f;
        float triR = r * 0.65f;
        float xL = cx - d;
        float xR = cx + d;
        float y = cy;
        AbstractGuiUtils.drawLine(stack, xL + triR, y, xR - triR, y, lw, color);
        AbstractGuiUtils.drawPolygon(stack, xL + triR, y, triR, 3, 180, color);
        AbstractGuiUtils.drawPolygon(stack, xR - triR, y, triR, 3, 0, color);
    }

    @Override
    public void update() {
        super.update();
        boolean hasCompletionEffects = !longPressCompletionEffects.isEmpty();
        if (!visible || !enabled) {
            if (hasCompletionEffects) {
                longPressCompletionEffects.clear();
            }
            longPressState.release();
            return;
        }
        if (longPressHandler == null && !hasCompletionEffects) {
            return;
        }
        if (hasCompletionEffects) {
            tickLongPressCompletionEffects();
        }
        if (longPressHandler == null) {
            longPressState.release();
            return;
        }
        if (longPressState.ready(longPressDurationMs)) {
            int dw = (int) width() - marginLeft - marginRight;
            int dh = (int) height() - marginTop - marginBottom;
            spawnLongPressCompletionFeedback(Math.max(1, dw), Math.max(1, dh));
            longPressState.fire();
            longPressHandler.accept(this);
            LOGGER.debug("Button long-pressed: id={}", id);
        }
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        boolean result = super.onMouseClick(event);
        if (event != null && event.button() == 0 && enabled) {
            if (longPressHandler != null) {
                longPressState.press(0);
                longPressCompletionEffects.clear();
            }
            result = true;
        }
        return result;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        boolean result = super.onMouseRelease(event, inside);
        boolean longPressFired = longPressState.fired();
        if (event != null && event.button() == 0) {
            longPressState.release();
        }
        if (event != null && event.button() == 0 && enabled && inside && !longPressFired && onClick != null) {
            onClick.accept(this);
            LOGGER.debug("Button clicked: id={}", id);
            result = true;
        }
        return result;
    }

    public ButtonWidget text(String text) {
        this.text = Text.literal(text);
        return this;
    }

    public ButtonWidget text(Component text) {
        this.text = Text.from(text);
        return this;
    }

    public ButtonWidget text(Text text) {
        this.text = text;
        return this;
    }

    @Override
    protected boolean onKeyPress(KeyEvent event) {
        if (!enabled || !focused) {
            return false;
        }

        int keyCode = event.keyCode();
        if (keyCode == GLFWKey.GLFW_KEY_ENTER || keyCode == GLFWKey.GLFW_KEY_KP_ENTER || keyCode == GLFWKey.GLFW_KEY_SPACE) {
            if (onClick != null) {
                onClick.accept(this);
                LOGGER.debug("Button activated by key: id={}, key={}", id, keyCode);
                return true;
            }
        }

        return false;
    }
}
