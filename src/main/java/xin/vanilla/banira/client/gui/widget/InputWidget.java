package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Style;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumTooltipTextureMode;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.CharInputEvent;
import xin.vanilla.banira.client.gui.event.KeyEvent;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 输入框
 */
@Accessors(chain = true, fluent = true)
public class InputWidget extends BaseWidget implements ITextWidget {
    private static long LAST_CLICK_TIME = 0L;
    /**
     * 按下时的区域：0=无，1=清空按钮，2=文本区域。抬起时需在同一区域才触发
     */
    protected int pressedArea = 0;

    /**
     * 为 true 时跳过文本内容渲染（供子类如 DropdownSelectWidget 的标签模式使用）
     */
    protected boolean skipTextContentForRendering = false;

    /**
     * 输入框文本
     */
    @Getter
    private String value = "";

    /**
     * 占位符文本
     */
    @Getter
    private Text hint;

    /**
     * 最大长度
     */
    @Getter
    @Setter
    private int maxLength = 32;

    /**
     * 是否可编辑
     */
    @Getter
    @Setter
    private boolean editable = true;

    /**
     * 密码模式仅改变显示和用户复制行为；表单回调仍可读取真实值。
     */
    @Getter
    @Setter
    private boolean password;

    /**
     * 是否错误状态
     */
    @Getter
    @Setter
    private boolean error;

    /**
     * 文本颜色
     */
    @Getter
    @Setter
    private int textColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputText();

    /**
     * 背景颜色
     */
    @Getter
    @Setter
    private int bgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputBg();

    /**
     * 错误状态时的背景颜色
     */
    @Getter
    @Setter
    private int errorBgColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputBgError();

    /**
     * 不可编辑时的文本颜色
     */
    @Getter
    @Setter
    private int uneditableTextColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputTextUneditable();

    /**
     * 提示文本颜色
     */
    @Getter
    @Setter
    private int hintColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputHint();

    /**
     * 光标颜色
     */
    @Getter
    @Setter
    private int cursorColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputCursor();

    /**
     * 文本字体大小
     */
    @Getter
    @Setter
    private float fontSize;

    /**
     * 提示文本字体大小
     */
    @Getter
    @Setter
    private float hintFontSize;

    /**
     * 边框颜色
     */
    @Getter
    @Setter
    private int borderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputBorder();

    /**
     * 焦点时的边框颜色
     */
    @Getter
    @Setter
    private int focusedBorderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputBorderFocused();

    /**
     * 禁用时的边框颜色
     */
    @Getter
    @Setter
    private int disabledBorderColor = BaniraColorConfig.forSeason(EnumSeason.AUTO).inputBorderDisabled();

    /**
     * 边框宽度
     */
    @Getter
    @Setter
    private int borderWidth = 0;

    /**
     * 内边距
     */
    @Getter
    @Setter
    private int paddingLeft = 5;

    @Getter
    @Setter
    private int paddingRight = 5;

    @Getter
    @Setter
    private int paddingTop = 2;

    @Getter
    @Setter
    private int paddingBottom = 2;

    /**
     * 外边距
     */
    @Getter
    @Setter
    private int marginLeft;

    @Getter
    @Setter
    private int marginRight;

    @Getter
    @Setter
    private int marginTop;

    @Getter
    @Setter
    private int marginBottom;

    @Override
    public boolean wantsScrollBeforeSiblings() {
        return true;
    }

    @Override
    public void applyTheme(BaniraColorConfig theme) {
        super.applyTheme(theme);
        textColor(theme.inputText()).bgColor(theme.inputBg()).errorBgColor(theme.inputBgError())
                .uneditableTextColor(theme.inputTextUneditable()).hintColor(theme.inputHint())
                .cursorColor(theme.inputCursor())
                .borderColor(theme.inputBorder()).focusedBorderColor(theme.inputBorderFocused())
                .disabledBorderColor(theme.inputBorderDisabled());
    }

    /**
     * 圆角半径
     */
    @Getter
    @Setter
    private int radius = 1;

    /**
     * 圆角半径
     */
    @Getter
    @Setter
    private ShapeDrawArgs.RoundedCornerMode cornerMode = ShapeDrawArgs.RoundedCornerMode.FINE;

    // region 光标

    /**
     * 光标位置
     */
    @Getter
    private int cursorPosition;

    /**
     * 高亮位置
     */
    @Getter
    private int highlightPos;

    /**
     * 显示位置（文本滚动）
     */
    @Getter
    private int displayPos;

    /**
     * 上一次的光标位置
     */
    private int lastCursorPos;

    /**
     * 是否按住 Shift
     */
    private boolean shiftPressed;

    /**
     * 方向键长按重复：当前按住的键码，-1 表示无
     */
    private int heldArrowKey = -1;
    /**
     * 方向键长按：上次重复触发时间（首次按下时记录）
     */
    private long lastArrowKeyRepeatTime = 0;
    /**
     * 是否已触发过首次重复（用于区分首次 500ms 与后续 50ms）
     */
    private boolean arrowKeyRepeatTriggered = false;
    private static final long ARROW_REPEAT_INITIAL_MS = 500;
    private static final long ARROW_REPEAT_INTERVAL_MS = 50;

    // endregion 光标

    /**
     * 文本格式化器
     */
    @Getter
    @Setter
    private BiFunction<String, Integer, FormattedCharSequence> formatter = (text, pos) ->
            FormattedCharSequence.forward(text, Style.EMPTY);

    /**
     * 文本变化回调
     */
    @Getter
    @Setter
    private Consumer<String> onTextChanged;

    /**
     * 输入验证回调
     */
    @Getter
    @Setter
    private Function<String, Boolean> validator;

    /**
     * 验证失败时的错误提示（鼠标悬浮于文本区域时显示，不包含清空按钮区域）
     */
    @Getter
    @Setter
    private String errorMessage;

    /**
     * 是否显示清空按钮（有内容时在右侧显示红色小圆×）
     */
    @Getter
    @Setter
    private boolean showClearButton = true;

    private static final int CLEAR_BUTTON_SIZE = 10;
    private static final int CLEAR_BUTTON_RADIUS = 4;
    /**
     * 滚轮横向滚动时每次移动的字符数（等效于按左右键）
     */
    private static final int SCROLL_STEP = 3;

    /**
     * 是否显示光标
     */
    @Getter
    private boolean cursorVisible = true;

    /**
     * 最后点击时间
     */
    private long lastClickTime = 0;

    /**
     * 撤销历史
     */
    private final Deque<String> undoHistory = new ArrayDeque<>();
    /**
     * 重做历史
     */
    private final Deque<String> redoHistory = new ArrayDeque<>();
    /**
     * 最大历史记录数
     */
    private static final int MAX_HISTORY_SIZE = 50;

    /**
     * 字体渲染
     */
    private final Font font;

    public InputWidget(BaniraScreen screen) {
        super(screen);
        this.font = AbstractGuiUtils.getFont();
        this.highlightPos = this.cursorPosition;
        this.lastCursorPos = this.cursorPosition;
        screen.registerFocusableWidget(this);
    }

    public InputWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
        this.font = AbstractGuiUtils.getFont();
        this.highlightPos = this.cursorPosition;
        this.lastCursorPos = this.cursorPosition;
        screen.registerFocusableWidget(this);
    }

    // region 渲染

    @Override
    public void render(PoseStack stack, float partialTicks) {
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

        this.updateDisplayPos();

        render(stack, x, y, width, height);

        renderChildren(stack, partialTicks);
    }

    private void render(PoseStack stack, int x, int y, int width, int height) {
        int drawX = x + marginLeft;
        int drawY = y + marginTop;
        int drawWidth = width - marginLeft - marginRight;
        int drawHeight = height - marginTop - marginBottom;

        int currentBgColor = this.error ? this.errorBgColor : this.bgColor;

        ShapeDrawArgs rect = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, currentBgColor);
        rect.rect().radius((float) radius).cornerMode(cornerMode);
        BaseShapeWidget.drawShape(rect);

        if (borderWidth > 0) {
            int currentBorderColor;
            if (!enabled) {
                currentBorderColor = disabledBorderColor;
            } else if (focused) {
                currentBorderColor = focusedBorderColor;
            } else {
                currentBorderColor = borderColor;
            }
            ShapeDrawArgs border = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, currentBorderColor);
            border.rect().radius((float) radius).cornerMode(cornerMode).border(borderWidth);
            BaseShapeWidget.drawShape(border);
        }

        float actualFontSize = fontSize > 0 ? fontSize : font.lineHeight;
        float fontScale = actualFontSize / font.lineHeight;

        if (!skipTextContentForRendering) {
            String rawValue = this.value;
            String value = displayValue(rawValue);
            int currentTextColor = this.textColor;
            if (!this.editable) {
                currentTextColor = this.uneditableTextColor;
            }
            int innerWidth = getTextAreaWidth(drawWidth);
            int scaledInnerWidth = fontScale != 1.0f ? (int) (innerWidth / fontScale) : innerWidth;

            int cursorPos = this.cursorPosition;
            int highlightPos = this.highlightPos;
            int displayPos = this.displayPos;

            String visibleText = this.font.plainSubstrByWidth(value.substring(displayPos), scaledInnerWidth);

            boolean hasLeftHidden = displayPos > 0;
            boolean hasRightHidden = displayPos + visibleText.length() < value.length();

            int cursorInVisible = cursorPos - displayPos;
            boolean cursorVisible = cursorInVisible >= 0 && cursorInVisible <= visibleText.length();
            boolean shouldShowCursor = this.focused() && ((System.currentTimeMillis() - LAST_CLICK_TIME) / 750) % 2 == 0 && cursorVisible;

            int textX = drawX + paddingLeft;
            int textY = drawY + (drawHeight - (int) actualFontSize + 1) / 2;

            int dotColor = currentTextColor;
            int centerY = textY + (int) actualFontSize / 2;
            if (hasLeftHidden) {
                int dotX = drawX + 2;
                AbstractGuiUtils.drawPixel(stack, dotX, centerY - 1, dotColor);
                AbstractGuiUtils.drawPixel(stack, dotX - 1, centerY, dotColor);
                AbstractGuiUtils.drawPixel(stack, dotX, centerY + 1, dotColor);
            }
            if (hasRightHidden) {
                int dotX = drawX + paddingLeft + innerWidth + 1;
                AbstractGuiUtils.drawPixel(stack, dotX, centerY - 1, dotColor);
                AbstractGuiUtils.drawPixel(stack, dotX + 1, centerY, dotColor);
                AbstractGuiUtils.drawPixel(stack, dotX, centerY + 1, dotColor);
            }

            int textDrawX = textX;
            if (!visibleText.isEmpty()) {
                String beforeCursor = cursorVisible ? visibleText.substring(0, Math.min(cursorInVisible, visibleText.length())) : visibleText;
                if (!beforeCursor.isEmpty()) {
                    if (fontScale != 1.0f) {
                        stack.pushPose();
                        stack.translate(textX, textY, 0);
                        stack.scale(fontScale, fontScale, 1.0f);
                        textDrawX = (int) (textX + this.font.draw(stack, this.formatter.apply(beforeCursor, displayPos),
                                0, 0, currentTextColor) * fontScale);
                        stack.popPose();
                    } else {
                        textDrawX = this.font.draw(stack, this.formatter.apply(beforeCursor, displayPos),
                                (float) textX, (float) textY, currentTextColor);
                    }
                }
            }

            boolean isAtEnd = cursorPos >= rawValue.length();
            int cursorX = textDrawX;
            if (!cursorVisible) {
                cursorX = cursorInVisible > 0 ? textX + innerWidth : textX;
            } else if (isAtEnd) {
                cursorX = textDrawX - 1;
                textDrawX = cursorX;
            }

            if (!visibleText.isEmpty() && cursorVisible && cursorInVisible < visibleText.length()) {
                String afterCursor = visibleText.substring(cursorInVisible);
                if (fontScale != 1.0f) {
                    stack.pushPose();
                    stack.translate(textDrawX, textY, 0);
                    stack.scale(fontScale, fontScale, 1.0f);
                    this.font.draw(stack, this.formatter.apply(afterCursor, cursorPos),
                            0, 0, currentTextColor);
                    stack.popPose();
                } else {
                    this.font.draw(stack, this.formatter.apply(afterCursor, cursorPos),
                            (float) textDrawX, (float) textY, currentTextColor);
                }
            }

            if (this.hint != null && value.isEmpty() && !this.focused()) {
                float actualHintFontSize = hintFontSize > 0 ? hintFontSize : font.lineHeight;
                float hintFontScale = actualHintFontSize / font.lineHeight;
                if (hintFontScale != 1.0f) {
                    stack.pushPose();
                    stack.translate(textX, textY, 0);
                    stack.scale(hintFontScale, hintFontScale, 1.0f);
                    FontDrawArgs args = FontDrawArgs.of(this.hint.stack(stack).color(hintColor)).x(0).y(0).maxWidth((int) (innerWidth / hintFontScale))
                            .wrap(false).position(EnumEllipsisPosition.END).maxLine(1);
                    LabelWidget.drawLimitedText(args);
                    stack.popPose();
                } else {
                    FontDrawArgs args = FontDrawArgs.of(this.hint.stack(stack).color(hintColor)).x(textX).y(textY).maxWidth(innerWidth)
                            .wrap(false).position(EnumEllipsisPosition.END).maxLine(1);
                    LabelWidget.drawLimitedText(args);
                }
            }

            if (highlightPos != cursorPos) {
                int highlightStart = Math.min(cursorPos, highlightPos);
                int highlightEnd = Math.max(cursorPos, highlightPos);

                int highlightStartInVisible = Math.max(0, highlightStart - displayPos);
                int highlightEndInVisible = Math.min(visibleText.length(), highlightEnd - displayPos);

                if (highlightStartInVisible < highlightEndInVisible) {
                    int highlightX1 = textX + (int) (this.font.width(visibleText.substring(0, highlightStartInVisible)) * fontScale);
                    int highlightX2 = textX + (int) (this.font.width(visibleText.substring(0, highlightEndInVisible)) * fontScale);
                    renderHighlight(stack, highlightX1, textY - 1, highlightX2, textY + (int) actualFontSize, textX, innerWidth);
                }
            }

            if (showClearButton && !value.isEmpty()) {
                int clearBtnW = CLEAR_BUTTON_SIZE + 2;
                int clearCenterX = drawX + drawWidth - clearBtnW / 2 - 1;
                int clearCenterY = drawY + drawHeight / 2;
                AbstractGuiUtils.drawCircle(stack, clearCenterX, clearCenterY, CLEAR_BUTTON_RADIUS, 0xFFE53935);
                drawClearIcon(stack, clearCenterX, clearCenterY, 0xFFFFFFFF);
            }

            if (showClearButton && !value.isEmpty() && isMouseOverClearButton() && screen != null) {
                double mx = screen.inputState().mouseX();
                double my = screen.inputState().mouseY();
                drawTooltipAtScreenCoords(stack, mx, my, Text.literal("清空"), EnumTooltipTextureMode.AUTO);
            } else if (error && errorMessage != null && !errorMessage.isEmpty() && isMouseOverTextArea() && screen != null) {
                double mx = screen.inputState().mouseX();
                double my = screen.inputState().mouseY();
                drawTooltipAtScreenCoords(stack, mx, my, Text.literal(errorMessage), EnumTooltipTextureMode.AUTO);
            }

            if (shouldShowCursor) {
                if (isAtEnd) {
                    if (fontScale != 1.0f) {
                        stack.pushPose();
                        stack.translate(cursorX, textY, 0);
                        stack.scale(fontScale, fontScale, 1.0f);
                        this.font.draw(stack, "_", 0, 0, currentTextColor);
                        stack.popPose();
                    } else {
                        this.font.draw(stack, "_", cursorX, textY, currentTextColor);
                    }
                } else {
                    int cursorHeight = (int) actualFontSize;
                    AbstractGuiUtils.fill(stack, cursorX, textY - 1, (int) Math.max(1, fontScale), cursorHeight, cursorColor);
                }
            }
        }
    }

    /**
     * 文本选择高亮颜色（半透明蓝）
     */
    private static final int SELECTION_HIGHLIGHT_COLOR = 0x40000080;

    /**
     * 绘制文本选择高亮。使用 PoseStack 确保在嵌套父级时坐标正确。
     */
    private void renderHighlight(PoseStack stack, int x1, int y1, int x2, int y2, int fieldX, int fieldWidth) {
        if (x1 > x2) {
            int temp = x1;
            x1 = x2;
            x2 = temp;
        }
        if (y1 > y2) {
            int temp = y1;
            y1 = y2;
            y2 = temp;
        }

        int maxX = fieldX + fieldWidth;
        if (x2 > maxX) {
            x2 = maxX;
        }
        if (x1 > maxX) {
            x1 = maxX;
        }

        int w = Math.max(0, x2 - x1);
        int h = Math.max(0, y2 - y1);
        if (w > 0 && h > 0) {
            AbstractGuiUtils.fill(stack, x1, y1, w, h, SELECTION_HIGHLIGHT_COLOR);
        }
    }

    /**
     * 获取内部宽度
     */
    private int getInnerWidth(int totalWidth) {
        return totalWidth - paddingLeft - paddingRight;
    }

    /**
     * 获取文本区域可用宽度（用于光标、截断、displayPos 等计算）。
     * 当 paddingRight 已包含清空按钮区域时（如 DropdownSelectWidget 设置的）不再重复扣除。
     */
    private int getTextAreaWidth(int totalWidth) {
        int base = getInnerWidth(totalWidth);
        if (showClearButton && !value.isEmpty() && paddingRight <= 10) {
            return base - (CLEAR_BUTTON_SIZE + 2);
        }
        return base;
    }

    private boolean isMouseOverClearButton() {
        if (renderCoordinate == null || screen == null) return false;
        int clearButtonWidth = (showClearButton && !value.isEmpty()) ? CLEAR_BUTTON_SIZE + 2 : 0;
        if (clearButtonWidth == 0) return false;
        double mx = screen.inputState().mouseX();
        double my = screen.inputState().mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();
        return mx >= absX + width - clearButtonWidth - marginRight && mx < absX + width - marginRight
                && my >= absY && my < absY + height;
    }

    /**
     * 鼠标是否在文本区域（排除清空按钮）
     */
    private boolean isMouseOverTextArea() {
        if (renderCoordinate == null || screen == null) return false;
        double mx = screen.inputState().mouseX();
        double my = screen.inputState().mouseY();
        double absX = absoluteX();
        double absY = absoluteY();
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();
        if (mx < absX || mx >= absX + width || my < absY || my >= absY + height) return false;
        int clearButtonWidth = (showClearButton && !value.isEmpty()) ? CLEAR_BUTTON_SIZE + 2 : 0;
        if (clearButtonWidth > 0 && mx >= absX + width - clearButtonWidth - marginRight) return false;
        return true;
    }

    /**
     * 在屏幕坐标绘制悬浮提示。使用延迟渲染避免 scissor 裁剪和层级被覆盖。默认跟随主题配置。
     */
    protected void drawTooltipAtScreenCoords(PoseStack stack, double mx, double my, Text text) {
        drawTooltipAtScreenCoords(stack, mx, my, text, EnumTooltipTextureMode.AUTO);
    }

    /**
     * 在屏幕坐标绘制悬浮提示。使用延迟渲染避免 scissor 裁剪和层级被覆盖。
     *
     * @param textureMode AUTO 时使用主题配置，TEXTURE/COLOR 时使用指定模式
     */
    protected void drawTooltipAtScreenCoords(PoseStack stack, double mx, double my, Text text, EnumTooltipTextureMode textureMode) {
        if (screen == null) return;
        if (screen instanceof BaniraScreen && ((BaniraScreen) screen).isAnyDropdownSelectOpen()) {
            return;
        }
        BaniraColorConfig theme = screen.getEffectiveTheme();
        EnumSeason season = screen.season();
        Text textToDraw = text;
        int mouseX = (int) mx;
        int mouseY = (int) my;
        boolean useTexture = textureMode == EnumTooltipTextureMode.AUTO
                ? theme.tooltipUseTexture()
                : (textureMode == EnumTooltipTextureMode.TEXTURE);
        screen.addDeferredTooltipRender(s -> {
            s.pushPose();
            s.last().pose().setIdentity();
            TooltipWidget.drawPopupMessage(s, FontDrawArgs.ofPopo(textToDraw.stack(s)).x(mouseX).y(mouseY).popupUseTexture(useTexture), theme, season);
            s.popPose();
        });
    }

    // endregion 渲染

    private static void drawClearIcon(PoseStack stack, int centerX, int centerY, int color) {
        float r = CLEAR_BUTTON_RADIUS * 0.4f; // x 略小于圆的 1/2
        AbstractGuiUtils.drawLine(stack, centerX - r, centerY - r, centerX + r, centerY + r, 1f, color);
        AbstractGuiUtils.drawLine(stack, centerX + r, centerY - r, centerX - r, centerY + r, 1f, color);
    }

    @Override
    public void update() {
        super.update();
        if (!visible || !enabled) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (focused() && heldArrowKey != -1 && screen != null && screen.inputState().isKeyPressed(heldArrowKey)) {
            long elapsed = currentTime - lastArrowKeyRepeatTime;
            long threshold = arrowKeyRepeatTriggered ? ARROW_REPEAT_INTERVAL_MS : ARROW_REPEAT_INITIAL_MS;
            if (elapsed >= threshold) {
                lastArrowKeyRepeatTime = currentTime;
                arrowKeyRepeatTriggered = true;
                this.shiftPressed = Screen.hasShiftDown();
                if (heldArrowKey == GLFWKey.GLFW_KEY_LEFT) {
                    if (Screen.hasControlDown()) moveCursorToWordStart();
                    else moveCursor(-1);
                } else if (heldArrowKey == GLFWKey.GLFW_KEY_RIGHT) {
                    if (Screen.hasControlDown()) moveCursorToWordEnd();
                    else moveCursor(1);
                }
            }
        } else {
            heldArrowKey = -1;
        }

        if (focused() && currentTime - lastClickTime > 0) {
            cursorVisible = ((currentTime - lastClickTime) / 750) % 2 == 0;
        }
    }

    // region 输入处理

    @Override
    protected boolean onMouseClick(MouseEvent event) {
        if (!visible || renderCoordinate == null || event == null || event.button() != 0) {
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();
        double absX = absoluteX();
        double absY = absoluteY();
        int clearButtonWidth = (showClearButton && !this.value.isEmpty()) ? CLEAR_BUTTON_SIZE + 2 : 0;

        boolean isInBounds = mouseX >= absX && mouseX < absX + width && mouseY >= absY && mouseY < absY + height;
        if (isInBounds) {
            boolean inClear = clearButtonWidth > 0 && mouseX >= absX + width - clearButtonWidth - marginRight;
            if (!inClear && isDoubleClick(event)) {
                selectWordAtCursor();
                return true;
            }
            pressedArea = inClear ? 1 : 2;
            return true;
        }
        pressedArea = 0;
        return false;
    }

    @Override
    protected boolean onMouseRelease(MouseEvent event, boolean inside) {
        if (!visible || renderCoordinate == null || event == null || event.button() != 0 || pressedArea == 0) {
            pressedArea = 0;
            return false;
        }
        double mouseX = event.mouseX();
        double mouseY = event.mouseY();
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();
        double absX = absoluteX();
        double absY = absoluteY();
        int clearButtonWidth = (showClearButton && !this.value.isEmpty()) ? CLEAR_BUTTON_SIZE + 2 : 0;
        boolean inClear = clearButtonWidth > 0 && mouseX >= absX + width - clearButtonWidth - marginRight;
        boolean inTextArea = !inClear && mouseX >= absX && mouseX < absX + width && mouseY >= absY && mouseY < absY + height;

        int releaseArea = inClear ? 1 : (inTextArea ? 2 : 0);
        boolean sameArea = releaseArea == pressedArea && inside;
        int area = pressedArea;
        pressedArea = 0;

        if (!sameArea) return area != 0;

        if (area == 1) {
            value("");
            return true;
        }
        if (area == 2) {
            LAST_CLICK_TIME = System.currentTimeMillis();
            int clickX = Mth.floor(mouseX) - (int) absX - marginLeft - paddingLeft;
            if (clickX < 0) clickX = 0;
            int textAreaWidth = getTextAreaWidth(width - marginLeft - marginRight);
            String visibleValue = displayValue(this.value);
            String visibleText = this.font.plainSubstrByWidth(visibleValue.substring(this.displayPos), textAreaWidth);
            int textPos = this.font.plainSubstrByWidth(visibleText, clickX).length() + this.displayPos;
            this.shiftPressed = Screen.hasShiftDown();
            moveCursorTo(textPos);
            if (!this.shiftPressed) this.highlightPos = textPos;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseScroll(MouseScrollEvent event) {
        if (!canConsumeInput() || value.isEmpty() || event == null) return false;
        if (renderCoordinate == null) return false;
        int totalWidth = (int) renderCoordinate.width() - marginLeft - marginRight;
        int innerWidth = getTextAreaWidth(totalWidth);
        if (this.font.width(displayValue(value)) <= innerWidth) return false;
        int step = event.delta() > 0 ? -SCROLL_STEP : SCROLL_STEP;
        moveCursor(step);
        return true;
    }

    @Override
    protected boolean onKeyPress(KeyEvent event) {
        if (!canConsumeInput()) {
            return false;
        }

        int keyCode = event.keyCode();
        this.shiftPressed = Screen.hasShiftDown();

        if (Screen.isSelectAll(keyCode)) {
            moveCursorTo(value.length());
            this.highlightPos = 0;
            this.updateDisplayPos();
            return true;
        }

        if (Screen.hasControlDown() && keyCode == GLFWKey.GLFW_KEY_Z && !Screen.hasShiftDown()) {
            undo();
            return true;
        }

        if ((Screen.hasControlDown() && keyCode == GLFWKey.GLFW_KEY_Y) ||
                (Screen.hasControlDown() && Screen.hasShiftDown() && keyCode == GLFWKey.GLFW_KEY_Z)) {
            redo();
            return true;
        }

        if (Screen.isCopy(keyCode)) {
            BaniraClientRuntime.clipboard(getHighlighted());
            return true;
        }

        if (Screen.isPaste(keyCode)) {
            if (this.editable) {
                this.saveToHistory();
                insertText(BaniraClientRuntime.clipboard());
            }
            return true;
        }

        if (Screen.isCut(keyCode)) {
            BaniraClientRuntime.clipboard(getHighlighted());
            if (this.editable) {
                this.saveToHistory();
                insertText("");
            }
            return true;
        }

        if (keyCode == GLFWKey.GLFW_KEY_LEFT) {
            heldArrowKey = GLFWKey.GLFW_KEY_LEFT;
            lastArrowKeyRepeatTime = System.currentTimeMillis();
            arrowKeyRepeatTriggered = false;
            if (Screen.hasControlDown()) {
                moveCursorToWordStart();
            } else {
                moveCursor(-1);
            }
            return true;
        }
        if (keyCode == GLFWKey.GLFW_KEY_RIGHT) {
            heldArrowKey = GLFWKey.GLFW_KEY_RIGHT;
            lastArrowKeyRepeatTime = System.currentTimeMillis();
            arrowKeyRepeatTriggered = false;
            if (Screen.hasControlDown()) {
                moveCursorToWordEnd();
            } else {
                moveCursor(1);
            }
            return true;
        }
        if (keyCode == GLFWKey.GLFW_KEY_HOME) {
            moveCursorTo(0);
            return true;
        }
        if (keyCode == GLFWKey.GLFW_KEY_END) {
            moveCursorTo(value.length());
            return true;
        }

        if (keyCode == GLFWKey.GLFW_KEY_BACKSPACE) {
            if (Screen.hasControlDown()) {
                deleteWords(-1);
            } else {
                deleteChars(-1);
            }
            return true;
        }
        if (keyCode == GLFWKey.GLFW_KEY_DELETE) {
            if (Screen.hasControlDown()) {
                deleteWords(1);
            } else {
                deleteChars(1);
            }
            return true;
        }

        this.updateDisplayPos();
        return false;
    }

    @Override
    protected boolean onKeyRelease(KeyEvent event) {
        int keyCode = event.keyCode();
        if (keyCode == heldArrowKey) {
            heldArrowKey = -1;
        }
        return false;
    }

    @Override
    protected boolean onCharTyped(CharInputEvent event) {
        if (!focused() || !enabled || !editable) {
            return false;
        }

        if (value.length() >= maxLength && getHighlighted().isEmpty()) {
            return true;
        }

        insertText(event.text());
        return true;
    }

    // endregion 输入处理

    // region 光标与选区操作

    /**
     * 设置输入框文本
     */
    public void value(String value) {
        if (value == null) value = "";
        String oldValue = this.value;
        this.value = value;
        if (!value.equals(oldValue)) {
            this.error = false;
        }
        int valueLength = value.length();
        this.displayPos = Mth.clamp(this.displayPos, 0, valueLength);
        this.highlightPos = Mth.clamp(this.highlightPos, 0, valueLength);
        this.cursorPosition = Mth.clamp(this.cursorPosition, 0, valueLength);
        this.updateDisplayPos();
        if (onTextChanged != null) {
            onTextChanged.accept(value);
        }
    }

    /**
     * 设置光标位置
     */
    public void setCursorPosition(int pos) {
        int newPos = Mth.clamp(pos, 0, value.length());
        if (this.cursorPosition != newPos) {
            long now = System.currentTimeMillis();
            LAST_CLICK_TIME = now;
            this.lastClickTime = now;
        }
        this.cursorPosition = newPos;
        if (!this.shiftPressed) {
            this.updateHighlightPos(this.cursorPosition);
        }
        this.updateDisplayPos();
    }

    /**
     * 移动光标到指定位置
     */
    public void moveCursorTo(int pos) {
        setCursorPosition(pos);
    }

    /**
     * 移动光标
     */
    public void moveCursor(int num) {
        int newPos = Mth.clamp(this.cursorPosition + num, 0, value.length());
        setCursorPosition(newPos);
    }

    /**
     * 移动到单词开始
     */
    private void moveCursorToWordStart() {
        if (cursorPosition <= 0) {
            return;
        }
        int pos = cursorPosition - 1;
        while (pos > 0 && Character.isWhitespace(value.charAt(pos - 1))) {
            pos--;
        }
        while (pos > 0 && !Character.isWhitespace(value.charAt(pos - 1))) {
            pos--;
        }
        moveCursorTo(pos);
    }

    /**
     * 移动到单词结尾
     */
    private void moveCursorToWordEnd() {
        if (cursorPosition >= value.length()) {
            return;
        }
        int pos = cursorPosition;
        while (pos < value.length() && !Character.isWhitespace(value.charAt(pos))) {
            pos++;
        }
        while (pos < value.length() && Character.isWhitespace(value.charAt(pos))) {
            pos++;
        }
        moveCursorTo(pos);
    }

    /**
     * 双击选词
     */
    private void selectWordAtCursor() {
        if (value.isEmpty()) {
            return;
        }
        int pos = Mth.clamp(cursorPosition, 0, value.length());
        int refPos = pos;
        if (pos < value.length() && StringUtils.isWordBoundaryWhitespace(value.charAt(pos))) {
            int idx = pos - 1;
            while (idx >= 0 && StringUtils.isWordBoundaryWhitespace(value.charAt(idx))) {
                idx--;
            }
            refPos = Math.max(idx, 0);
        } else if (pos > 0) {
            refPos = pos - 1;
        }
        int wordStart = StringUtils.findTokenStart(value, refPos);
        int wordEnd = StringUtils.findTokenEnd(value, refPos);
        this.cursorPosition = wordStart;
        this.highlightPos = wordEnd;
        updateDisplayPos();
    }

    /**
     * 更新高亮位置
     */
    private void updateHighlightPos(int pos) {
        String rawValue = this.value;
        String value = displayValue(rawValue);
        int valueLength = rawValue.length();
        this.highlightPos = Mth.clamp(pos, 0, valueLength);
        this.updateDisplayPos();
    }

    /**
     * 更新显示位置
     */
    private void updateDisplayPos() {
        if (renderCoordinate == null) {
            return;
        }

        String value = this.value;
        int valueLength = value.length();
        int cursorPos = this.cursorPosition;
        int drawWidth = (int) renderCoordinate.width() - marginLeft - marginRight;
        int innerWidth = getTextAreaWidth(drawWidth);

        if (this.displayPos > valueLength) {
            this.displayPos = valueLength;
        }

        if (innerWidth <= 0) {
            return;
        }
        if (this.font.width(value) <= innerWidth) {
            this.displayPos = 0;
            return;
        }

        boolean movingRight = cursorPos > this.lastCursorPos;
        boolean movingLeft = cursorPos < this.lastCursorPos;
        if (cursorPos != this.lastCursorPos) {
            this.lastCursorPos = cursorPos;
        }

        String remainingText = value.substring(this.displayPos);
        String visibleText = this.font.plainSubstrByWidth(remainingText, innerWidth);
        int visibleEnd = this.displayPos + visibleText.length();

        boolean hasLeftHidden = this.displayPos > 0;
        boolean hasRightHidden = visibleEnd < valueLength;

        int cursorInVisible = cursorPos - this.displayPos;

        if (cursorPos < this.displayPos) {
            String beforeCursor = value.substring(0, cursorPos);
            String reverseText = this.font.plainSubstrByWidth(beforeCursor, innerWidth, true);
            this.displayPos = Math.max(0, cursorPos - reverseText.length());
        } else if (cursorPos > visibleEnd) {
            String beforeCursor = value.substring(0, cursorPos);
            String reverseText = this.font.plainSubstrByWidth(beforeCursor, innerWidth, true);
            this.displayPos = Math.max(0, cursorPos - reverseText.length());
        } else {
            int lenVisible = visibleText.length();
            if (movingRight && hasRightHidden && lenVisible > 0) {
                int secondLastIndex = Math.max(0, lenVisible - 2);
                if (cursorInVisible >= secondLastIndex) {
                    this.displayPos = Math.min(this.displayPos + 1, valueLength);
                }
            } else if (movingLeft && hasLeftHidden && lenVisible > 0) {
                int secondIndex = 1;
                if (cursorInVisible <= secondIndex) {
                    this.displayPos = Math.max(this.displayPos - 1, 0);
                }
            }
        }

        if (this.highlightPos != cursorPos) {
            visibleText = this.font.plainSubstrByWidth(value.substring(this.displayPos), innerWidth);
            visibleEnd = visibleText.length() + this.displayPos;

            boolean cursorVisible = cursorInVisible >= 0 && cursorInVisible <= visibleText.length();

            if (cursorVisible) {
                if (this.highlightPos < this.displayPos) {
                    String beforeHighlight = value.substring(0, this.highlightPos);
                    String reverseText = this.font.plainSubstrByWidth(beforeHighlight, innerWidth, true);
                    int newDisplayPos = Math.max(0, this.highlightPos - reverseText.length());
                    // 确保光标仍然可见
                    if (cursorPos >= newDisplayPos) {
                        String newVisibleText = this.font.plainSubstrByWidth(value.substring(newDisplayPos), innerWidth);
                        if (cursorPos <= newDisplayPos + newVisibleText.length()) {
                            this.displayPos = newDisplayPos;
                        }
                    }
                } else if (this.highlightPos > visibleEnd) {
                    String beforeHighlight = value.substring(0, this.highlightPos);
                    String reverseText = this.font.plainSubstrByWidth(beforeHighlight, innerWidth, true);
                    int newDisplayPos = Math.max(0, this.highlightPos - reverseText.length());
                    // 确保光标仍然可见
                    if (cursorPos >= newDisplayPos) {
                        String newVisibleText = this.font.plainSubstrByWidth(value.substring(newDisplayPos), innerWidth);
                        if (cursorPos <= newDisplayPos + newVisibleText.length()) {
                            this.displayPos = newDisplayPos;
                        }
                    }
                }
            }
        }

        this.displayPos = Mth.clamp(this.displayPos, 0, valueLength);
    }

    @Override
    public void focused(boolean focused) {
        super.focused(focused);
        long currentTime = System.currentTimeMillis();
        if (focused) {
            this.lastClickTime = currentTime;
            this.cursorVisible = true;
        } else {
            this.highlightPos = this.cursorPosition;
            this.shiftPressed = false;
        }
    }

    /**
     * 插入文本
     */
    public void insertText(String text) {
        if (!this.editable) {
            return;
        }

        String value = this.value;
        int valueLen = value.length();
        int cursorPos = Mth.clamp(this.cursorPosition, 0, valueLen);
        int highlightPos = Mth.clamp(this.highlightPos, 0, valueLen);
        int start = Math.min(cursorPos, highlightPos);
        int end = Math.max(cursorPos, highlightPos);
        boolean hasSelection = start != end;

        if (hasSelection) {
            this.saveToHistory();

            String newValue = value.substring(0, start) + text + value.substring(end);
            if (newValue.length() > this.maxLength) {
                newValue = newValue.substring(0, this.maxLength);
            }

            value(newValue);
            this.error = false;
            int newCursorPos = Math.min(start + text.length(), newValue.length());
            setCursorPosition(newCursorPos);
            this.highlightPos = newCursorPos;
            this.lastCursorPos = newCursorPos;
        } else {
            if (!text.isEmpty()) {
                this.saveToHistory();
                this.error = false;
            }
            String newValue = value.substring(0, cursorPos) + text + value.substring(cursorPos);
            if (newValue.length() > this.maxLength) {
                newValue = newValue.substring(0, this.maxLength);
            }
            value(newValue);
            int newCursorPos = Math.min(cursorPos + text.length(), newValue.length());
            setCursorPosition(newCursorPos);
            this.highlightPos = newCursorPos;
            this.lastCursorPos = newCursorPos;
        }
        this.updateDisplayPos();
    }

    /**
     * 删除单词
     */
    public void deleteWords(int num) {
        if (!this.editable || this.value.isEmpty()) {
            return;
        }

        // 删除选中的文本
        if (this.highlightPos != this.cursorPosition) {
            this.saveToHistory();
            this.insertText("");
            return;
        }

        // 删除单词
        String oldValue = this.value;
        this.saveToHistory();
        if (num < 0) {
            int start = Math.max(0, cursorPosition - 1);
            while (start > 0 && Character.isWhitespace(value.charAt(start - 1))) {
                start--;
            }
            while (start > 0 && !Character.isWhitespace(value.charAt(start - 1))) {
                start--;
            }
            value(value.substring(0, start) + value.substring(cursorPosition));
            setCursorPosition(start);
        } else {
            int end = Math.min(value.length(), cursorPosition);
            while (end < value.length() && Character.isWhitespace(value.charAt(end))) {
                end++;
            }
            while (end < value.length() && !Character.isWhitespace(value.charAt(end))) {
                end++;
            }
            value(value.substring(0, cursorPosition) + value.substring(end));
        }
        if (!this.value.equals(oldValue)) {
            this.error = false;
        }
        // 重置高亮位置并更新显示位置
        this.highlightPos = this.cursorPosition;
        this.lastCursorPos = this.cursorPosition;
        this.updateDisplayPos();
    }

    /**
     * 删除字符
     */
    public void deleteChars(int num) {
        if (!this.editable || this.value.isEmpty() || num == 0) {
            return;
        }

        if (this.highlightPos != this.cursorPosition) {
            this.saveToHistory();
            this.insertText("");
            return;
        }

        String oldValue = this.value;
        this.saveToHistory();
        if (num < 0) {
            int start = Math.max(0, cursorPosition + num);
            value(value.substring(0, start) + value.substring(cursorPosition));
            setCursorPosition(start);
        } else {
            int end = Math.min(value.length(), cursorPosition + num);
            value(value.substring(0, cursorPosition) + value.substring(end));
        }
        if (!this.value.equals(oldValue)) {
            this.error = false;
        }
        this.highlightPos = this.cursorPosition;
        this.lastCursorPos = this.cursorPosition;
        this.updateDisplayPos();
    }

    /**
     * 获取选中的文本
     */
    @ParametersAreNonnullByDefault
    public String getHighlighted() {
        int start = Math.min(this.cursorPosition, this.highlightPos);
        int end = Math.max(this.cursorPosition, this.highlightPos);
        String value = this.value;
        if (start >= 0 && end <= value.length() && start < end) {
            String selected = value.substring(start, end);
            return password ? mask(selected.length()) : selected;
        }
        return "";
    }

    /** 返回与真实文本等长的掩码，避免渲染和剪贴板泄露密码。 */
    private String displayValue(String rawValue) {
        return password ? mask(rawValue.length()) : rawValue;
    }

    private static String mask(int length) {
        if (length <= 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append('*');
        }
        return result.toString();
    }

    /**
     * 检查是否可以消费输入
     */
    public boolean canConsumeInput() {
        return this.visible && this.focused() && this.editable;
    }


    // endregion 光标与选区操作

    // region 历史
    private void saveToHistory() {
        String currentValue = this.value;
        if (!this.undoHistory.isEmpty() && this.undoHistory.peekLast().equals(currentValue)) {
            return;
        }

        this.undoHistory.offerLast(currentValue);
        if (this.undoHistory.size() > MAX_HISTORY_SIZE) {
            this.undoHistory.pollFirst();
        }

        this.redoHistory.clear();
    }

    /**
     * 撤销操作
     */
    private void undo() {
        if (this.undoHistory.isEmpty()) {
            return;
        }

        String currentValue = this.value;
        // 将当前值添加到重做历史
        this.redoHistory.offerLast(currentValue);
        if (this.redoHistory.size() > MAX_HISTORY_SIZE) {
            this.redoHistory.pollFirst();
        }

        String previousValue = this.undoHistory.pollLast();
        if (previousValue != null) {
            value(previousValue);
            this.error = false;
            int cursorPos = Math.min(this.cursorPosition, previousValue.length());
            setCursorPosition(cursorPos);
            this.highlightPos = cursorPos;
            this.updateDisplayPos();
        }
    }

    /**
     * 重做操作
     */
    private void redo() {
        if (this.redoHistory.isEmpty()) {
            return;
        }

        String currentValue = this.value;
        this.undoHistory.offerLast(currentValue);
        if (this.undoHistory.size() > MAX_HISTORY_SIZE) {
            this.undoHistory.pollFirst();
        }

        String nextValue = this.redoHistory.pollLast();
        if (nextValue != null) {
            value(nextValue);
            this.error = false;
            int cursorPos = Math.min(this.cursorPosition, nextValue.length());
            setCursorPosition(cursorPos);
            this.highlightPos = cursorPos;
            this.updateDisplayPos();
        }
    }

    // endregion 历史

    @Override
    public ITextWidget text(String text) {
        this.hint = Text.literal(text);
        return this;
    }

    @Override
    public ITextWidget text(Component text) {
        this.hint = Text.from(text);
        return this;
    }

    @Override
    public ITextWidget text(Text text) {
        this.hint = text;
        return this;
    }

    @Override
    public Text text() {
        return this.hint;
    }
}
