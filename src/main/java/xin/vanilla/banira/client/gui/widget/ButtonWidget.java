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
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;

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

    private static final Logger LOGGER = LogManager.getLogger();

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

    private boolean longPressHandlerFired;

    @Getter
    @Setter
    private int bgColor = BaniraColorConfig.winter().buttonBg();

    @Getter
    @Setter
    private int hoverBgColor = BaniraColorConfig.winter().buttonBgHover();

    @Getter
    @Setter
    private int focusedBgColor = BaniraColorConfig.winter().buttonBgFocused();

    @Getter
    @Setter
    private int pressedBgColor = BaniraColorConfig.winter().buttonBgPressed();

    @Getter
    @Setter
    private int disabledBgColor = BaniraColorConfig.winter().buttonBgDisabled();

    @Getter
    @Setter
    private int borderColor = BaniraColorConfig.winter().buttonBorder();

    @Getter
    @Setter
    private int hoverBorderColor = BaniraColorConfig.winter().buttonBorderHover();

    @Getter
    @Setter
    private int focusedBorderColor = BaniraColorConfig.winter().buttonBorderFocused();

    @Getter
    @Setter
    private int pressedBorderColor = BaniraColorConfig.winter().buttonBorderPressed();

    @Getter
    @Setter
    private int disabledBorderColor = BaniraColorConfig.winter().buttonBorderDisabled();

    @Getter
    @Setter
    private int borderWidth = 1;

    @Getter
    @Setter
    private int textColor = BaniraColorConfig.winter().buttonText();

    @Getter
    @Setter
    private int hoverTextColor = BaniraColorConfig.winter().buttonTextHover();

    @Getter
    @Setter
    private int focusedTextColor = BaniraColorConfig.winter().buttonTextFocused();

    @Getter
    @Setter
    private int pressedTextColor = BaniraColorConfig.winter().buttonTextPressed();

    @Getter
    @Setter
    private int disabledTextColor = BaniraColorConfig.winter().buttonTextDisabled();

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
    private EnumEllipsisPosition textEllipsisPosition = EnumEllipsisPosition.NONE;

    /**
     * 预置图标样式，非 null 时绘制图标而非文本
     */
    @Getter
    private PresetStyle presetStyle = null;

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
                .disabledTextColor(theme.buttonTextDisabled());
    }

    @Getter
    @Setter
    private float radius = 2.0f;

    @Getter
    @Setter
    private ShapeDrawArgs.RoundedCornerMode cornerMode = ShapeDrawArgs.RoundedCornerMode.FINE;

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

        boolean longPressProgress = enabled && longPressHandler != null && mousePressed && pressedMouseButton == 0;

        int currentBgColor;
        int currentBorderColor;
        int currentTextColor;
        int currentIconColor;

        if (longPressProgress) {
            float progress = longPressHandlerFired
                    ? 1f
                    : Math.min(1f, (System.currentTimeMillis() - mousePressStartMillis()) / (float) longPressDurationMs);
            int fillW = Math.max(0, Math.min(drawWidth, (int) Math.ceil(drawWidth * progress)));

            ShapeDrawArgs track = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, hoverBgColor);
            track.rect().radius(radius).cornerMode(cornerMode);
            BaseShapeWidget.drawShape(track);

            if (fillW > 0) {
                int ax = (int) Math.round(absoluteX()) + marginLeft;
                int ay = (int) Math.round(absoluteY()) + marginTop;
                AbstractGuiUtils.pushScissor(ax, ay, fillW, drawHeight);
                ShapeDrawArgs fill = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, pressedBgColor);
                fill.rect().radius(radius).cornerMode(cornerMode);
                BaseShapeWidget.drawShape(fill);
                AbstractGuiUtils.popScissor();
            }

            if (borderWidth > 0) {
                currentBorderColor = hoverBorderColor;
                ShapeDrawArgs border = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, currentBorderColor);
                border.rect().radius(radius).cornerMode(cornerMode).border(borderWidth);
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
            rect.rect().radius(radius).cornerMode(cornerMode);
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
                border.rect().radius(radius).cornerMode(cornerMode).border(borderWidth);
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

        renderChildren(stack, partialTicks);
    }

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
        if (!visible || !enabled || longPressHandler == null) {
            return;
        }
        if (mousePressed && pressedMouseButton == 0 && !longPressHandlerFired) {
            if (System.currentTimeMillis() - mousePressStartMillis() >= longPressDurationMs) {
                longPressHandlerFired = true;
                longPressHandler.accept(this);
                LOGGER.debug("Button long-pressed: id={}", id);
            }
        }
    }

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        boolean result = super.onMouseClick(event);
        if (event != null && event.button() == 0 && enabled) {
            if (longPressHandler != null) {
                longPressHandlerFired = false;
            }
            result = true;
        }
        return result;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        boolean result = super.onMouseRelease(event, inside);
        if (event != null && event.button() == 0 && enabled && inside && !longPressHandlerFired && onClick != null) {
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
    protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!enabled || !focused) {
            return false;
        }

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
