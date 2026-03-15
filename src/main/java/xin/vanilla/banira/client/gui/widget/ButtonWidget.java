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
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;

import java.util.function.Consumer;

/**
 * 按钮Widget
 */
@Accessors(chain = true, fluent = true)
public class ButtonWidget extends BaseWidget implements ITextWidget {
    private static final Logger LOGGER = LogManager.getLogger();

    @Getter
    private Text text = Text.empty();

    @Getter
    @Setter
    private Consumer<ButtonWidget> onClick;

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

        int currentBgColor;
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
            int currentBorderColor;
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

        FontRenderer font = AbstractGuiUtils.getFont();

        int currentTextColor;
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

        int textX = drawX + paddingLeft;
        int textY = drawY + paddingTop;
        int availableWidth = drawWidth - paddingLeft - paddingRight;
        int availableHeight = drawHeight - paddingTop - paddingBottom;

        FontDrawArgs drawArgs = FontDrawArgs.of(text.color(currentTextColor));
        if (textMaxWidth > 0 && textEllipsisPosition != EnumEllipsisPosition.NONE) {
            LabelWidget.drawLimitedText(drawArgs.x(textX).y(textY + (availableHeight - 9) / 2)
                    .maxWidth(Math.min(textMaxWidth, availableWidth))
                    .position(textEllipsisPosition));
        } else if (fontSize != 9.0f) {
            stack.pushPose();
            stack.translate(textX, textY, 0);
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
            int centeredTextX = textX + (availableWidth - textWidth) / 2;
            int centeredTextY = textY + (availableHeight - textHeight) / 2;
            LabelWidget.drawLimitedText(drawArgs.x(centeredTextX).y(centeredTextY));
        }

        renderChildren(stack, partialTicks);
    }

    @Override
    public void update() {
        super.update();
        if (!visible || !enabled) {
            return;
        }
    }

    @Override
    protected boolean onMouseClick(double mouseX, double mouseY, int mouseButton) {
        boolean result = super.onMouseClick(mouseX, mouseY, mouseButton);
        if (mouseButton == 0 && enabled) {
            result = true;
        }
        return result;
    }

    @Override
    protected boolean onMouseRelease(double mouseX, double mouseY, int mouseButton, boolean inside) {
        boolean result = super.onMouseRelease(mouseX, mouseY, mouseButton, inside);
        if (mouseButton == 0 && enabled && inside) {
            if (onClick != null) {
                onClick.accept(this);
                LOGGER.debug("Button clicked: id={}", id);
                result = true;
            }
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
