package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.Style;
import org.lwjgl.opengl.GL11C;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;

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
    private int textColor = BaniraColorConfig.winter().inputText();

    /**
     * 背景颜色
     */
    @Getter
    @Setter
    private int bgColor = BaniraColorConfig.winter().inputBg();

    /**
     * 错误状态时的背景颜色
     */
    @Getter
    @Setter
    private int errorBgColor = BaniraColorConfig.winter().inputBgError();

    /**
     * 不可编辑时的文本颜色
     */
    @Getter
    @Setter
    private int uneditableTextColor = BaniraColorConfig.winter().inputTextUneditable();

    /**
     * 提示文本颜色
     */
    @Getter
    @Setter
    private int hintColor = BaniraColorConfig.winter().inputHint();

    /**
     * 光标颜色
     */
    @Getter
    @Setter
    private int cursorColor = BaniraColorConfig.winter().inputCursor();

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
    private int borderColor = BaniraColorConfig.winter().inputBorder();

    /**
     * 焦点时的边框颜色
     */
    @Getter
    @Setter
    private int focusedBorderColor = BaniraColorConfig.winter().inputBorderFocused();

    /**
     * 禁用时的边框颜色
     */
    @Getter
    @Setter
    private int disabledBorderColor = BaniraColorConfig.winter().inputBorderDisabled();

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
     * 文本格式化器
     */
    @Getter
    @Setter
    private BiFunction<String, Integer, IReorderingProcessor> formatter = (text, pos) ->
            IReorderingProcessor.forward(text, Style.EMPTY);

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
     * 是否显示清空按钮（有内容时在右侧显示红色小圆×）
     */
    @Getter
    @Setter
    private boolean showClearButton = true;

    private static final int CLEAR_BUTTON_SIZE = 10;
    private static final int CLEAR_BUTTON_RADIUS = 4;

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
    private final FontRenderer font;

    public InputWidget(BaniraScreen screen) {
        super(screen);
        this.font = Minecraft.getInstance().font;
        this.highlightPos = this.cursorPosition;
        this.lastCursorPos = this.cursorPosition;
        // 注册为可聚焦的Widget
        screen.registerFocusableWidget(this);
    }

    public InputWidget(BaniraScreen screen, ScreenCoordinate bounds) {
        super(screen, bounds);
        this.font = Minecraft.getInstance().font;
        this.highlightPos = this.cursorPosition;
        this.lastCursorPos = this.cursorPosition;
        // 注册为可聚焦的Widget
        screen.registerFocusableWidget(this);
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

        this.updateDisplayPos();

        render(stack, x, y, width, height);

        // 渲染子Widget
        renderChildren(stack, partialTicks);
    }

    private void render(MatrixStack stack, int x, int y, int width, int height) {
        // 计算实际绘制区域（考虑外边距）
        int drawX = x + marginLeft;
        int drawY = y + marginTop;
        int drawWidth = width - marginLeft - marginRight;
        int drawHeight = height - marginTop - marginBottom;

        // 确定背景颜色
        int currentBgColor = this.error ? this.errorBgColor : this.bgColor;

        // 绘制背景（使用ShapeDrawArgs，参考ButtonWidget）
        ShapeDrawArgs rect = ShapeDrawArgs.rect(stack, drawX, drawY, drawWidth, drawHeight, currentBgColor);
        rect.rect().radius((float) radius).cornerMode(cornerMode);
        BaseShapeWidget.drawShape(rect);

        // 绘制边框
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

        // 计算实际字体大小（如果为0则使用font.lineHeight）
        float actualFontSize = fontSize > 0 ? fontSize : font.lineHeight;
        float fontScale = actualFontSize / font.lineHeight;

        String value = this.value;
        int currentTextColor = this.textColor;
        if (!this.editable) {
            currentTextColor = this.uneditableTextColor;
        }
        int innerWidth = getTextAreaWidth(drawWidth);
        // 计算缩放后的可用宽度（用于文本截断）
        int scaledInnerWidth = fontScale != 1.0f ? (int) (innerWidth / fontScale) : innerWidth;

        int cursorPos = this.cursorPosition;
        int highlightPos = this.highlightPos;
        int displayPos = this.displayPos;

        // 获取可见文本（使用缩放后的宽度）
        String visibleText = this.font.plainSubstrByWidth(value.substring(displayPos), scaledInnerWidth);

        // 是否存在左右未显示文本
        boolean hasLeftHidden = displayPos > 0;
        boolean hasRightHidden = displayPos + visibleText.length() < value.length();

        // 计算光标在可见文本中的位置
        int cursorInVisible = cursorPos - displayPos;
        boolean cursorVisible = cursorInVisible >= 0 && cursorInVisible <= visibleText.length();
        // 光标缓慢闪烁
        boolean shouldShowCursor = this.focused() && ((System.currentTimeMillis() - LAST_CLICK_TIME) / 750) % 2 == 0 && cursorVisible;

        // 计算文本绘制位置
        int textX = drawX + paddingLeft;
        int textY = drawY + (drawHeight - (int) actualFontSize + 1) / 2;

        // 绘制溢出内容标记
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

        // 光标之前的文本
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

        // 计算光标位置
        boolean isAtEnd = cursorPos >= value.length();
        int cursorX = textDrawX;
        if (!cursorVisible) {
            cursorX = cursorInVisible > 0 ? textX + innerWidth : textX;
        } else if (isAtEnd) {
            cursorX = textDrawX - 1;
            textDrawX = cursorX;
        }

        // 光标之后的文本
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

        // 提示文本
        if (this.hint != null && value.isEmpty() && !this.focused()) {
            float actualHintFontSize = hintFontSize > 0 ? hintFontSize : font.lineHeight;
            float hintFontScale = actualHintFontSize / font.lineHeight;
            if (hintFontScale != 1.0f) {
                stack.pushPose();
                stack.translate(textX, textY, 0);
                stack.scale(hintFontScale, hintFontScale, 1.0f);
                FontDrawArgs args = FontDrawArgs.of(this.hint.stack(stack).color(hintColor)).x(0).y(0).maxWidth((int) (innerWidth / hintFontScale));
                LabelWidget.drawLimitedText(args);
                stack.popPose();
            } else {
                FontDrawArgs args = FontDrawArgs.of(this.hint.stack(stack).color(hintColor)).x(textX).y(textY).maxWidth(innerWidth);
                LabelWidget.drawLimitedText(args);
            }
        }

        // 绘制文本选择高亮
        if (highlightPos != cursorPos) {
            // 计算高亮的起始和结束位置
            int highlightStart = Math.min(cursorPos, highlightPos);
            int highlightEnd = Math.max(cursorPos, highlightPos);

            // 计算高亮在可见文本中的起始和结束位置
            int highlightStartInVisible = Math.max(0, highlightStart - displayPos);
            int highlightEndInVisible = Math.min(visibleText.length(), highlightEnd - displayPos);

            if (highlightStartInVisible < highlightEndInVisible) {
                // 计算高亮的屏幕坐标
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
            TooltipWidget.drawPopupMessage(stack, FontDrawArgs.ofPopo(Text.literal("清空").stack(stack)).x((int) mx).y((int) my),
                    screen.getEffectiveTheme(), screen.season());
        }

        // 绘制光标
        if (shouldShowCursor) {
            if (isAtEnd) {
                // 在文本末尾绘制下划线光标
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
                // 在文本中间绘制竖线光标
                int cursorHeight = (int) actualFontSize;
                AbstractGuiUtils.fill(stack, cursorX, textY - 1, (int) Math.max(1, fontScale), cursorHeight, cursorColor);
            }
        }
    }

    /**
     * 绘制文本选择高亮
     */
    private void renderHighlight(MatrixStack stack, int x1, int y1, int x2, int y2, int fieldX, int fieldWidth) {
        // 确保坐标顺序正确
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

        // 限制在输入框范围内
        int maxX = fieldX + fieldWidth;
        if (x2 > maxX) {
            x2 = maxX;
        }
        if (x1 > maxX) {
            x1 = maxX;
        }

        // 使用逻辑运算绘制高亮
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuilder();
        RenderSystem.color4f(0.0F, 0.0F, 1.0F, 1.0F);
        RenderSystem.disableTexture();
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        bufferbuilder.begin(GL11C.GL_QUADS, DefaultVertexFormats.POSITION);
        bufferbuilder.vertex(x1, y2, 0.0D).endVertex();
        bufferbuilder.vertex(x2, y2, 0.0D).endVertex();
        bufferbuilder.vertex(x2, y1, 0.0D).endVertex();
        bufferbuilder.vertex(x1, y1, 0.0D).endVertex();
        tessellator.end();
        RenderSystem.disableColorLogicOp();
        RenderSystem.enableTexture();
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

    private static void drawClearIcon(MatrixStack stack, int centerX, int centerY, int color) {
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

        // 更新光标闪烁
        long currentTime = System.currentTimeMillis();
        if (focused() && currentTime - lastClickTime > 0) {
            cursorVisible = ((currentTime - lastClickTime) / 750) % 2 == 0;
        }
    }

    @Override
    protected boolean onMouseClick(double mouseX, double mouseY, int mouseButton) {
        if (!visible || renderCoordinate == null || mouseButton != 0) {
            return false;
        }
        int width = (int) renderCoordinate.width();
        int height = (int) renderCoordinate.height();
        double absX = absoluteX();
        double absY = absoluteY();
        int clearButtonWidth = (showClearButton && !this.value.isEmpty()) ? CLEAR_BUTTON_SIZE + 2 : 0;

        boolean isInBounds = mouseX >= absX && mouseX < absX + width && mouseY >= absY && mouseY < absY + height;
        if (isInBounds) {
            boolean inClear = clearButtonWidth > 0 && mouseX >= absX + width - clearButtonWidth - marginRight;
            pressedArea = inClear ? 1 : 2;
            return true;
        }
        pressedArea = 0;
        return false;
    }

    @Override
    protected boolean onMouseRelease(double mouseX, double mouseY, int mouseButton, boolean inside) {
        if (!visible || renderCoordinate == null || mouseButton != 0 || pressedArea == 0) {
            pressedArea = 0;
            return false;
        }
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
            int clickX = MathHelper.floor(mouseX) - (int) absX - marginLeft - paddingLeft;
            if (clickX < 0) clickX = 0;
            int textAreaWidth = getTextAreaWidth(width - marginLeft - marginRight);
            String visibleText = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), textAreaWidth);
            int textPos = this.font.plainSubstrByWidth(visibleText, clickX).length() + this.displayPos;
            this.shiftPressed = Screen.hasShiftDown();
            moveCursorTo(textPos);
            if (!this.shiftPressed) this.highlightPos = textPos;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!canConsumeInput()) {
            return false;
        }

        this.shiftPressed = Screen.hasShiftDown();

        // 处理 全选
        if (Screen.isSelectAll(keyCode)) {
            moveCursorTo(value.length());
            this.highlightPos = 0;
            this.updateDisplayPos();
            return true;
        }

        // 处理 Ctrl+Z 撤销
        if (Screen.hasControlDown() && keyCode == GLFWKey.GLFW_KEY_Z && !Screen.hasShiftDown()) {
            undo();
            return true;
        }

        // 处理 Ctrl+Y Ctrl+Shift+Z 重做
        if ((Screen.hasControlDown() && keyCode == GLFWKey.GLFW_KEY_Y) ||
                (Screen.hasControlDown() && Screen.hasShiftDown() && keyCode == GLFWKey.GLFW_KEY_Z)) {
            redo();
            return true;
        }

        // 处理 复制
        if (Screen.isCopy(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(getHighlighted());
            return true;
        }

        // 处理 粘贴
        if (Screen.isPaste(keyCode)) {
            if (this.editable) {
                this.saveToHistory();
                insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            }
            return true;
        }

        // 处理 剪切
        if (Screen.isCut(keyCode)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(getHighlighted());
            if (this.editable) {
                this.saveToHistory();
                insertText("");
            }
            return true;
        }

        // 处理方向
        if (keyCode == GLFWKey.GLFW_KEY_LEFT) {
            if (Screen.hasControlDown()) {
                // Ctrl+Left: 移动到单词开始
                moveCursorToWordStart();
            } else {
                moveCursor(-1);
            }
            return true;
        }
        if (keyCode == GLFWKey.GLFW_KEY_RIGHT) {
            if (Screen.hasControlDown()) {
                // Ctrl+Right: 移动到单词结尾
                moveCursorToWordEnd();
            } else {
                moveCursor(1);
            }
            return true;
        }
        if (!Screen.hasShiftDown() && keyCode == GLFWKey.GLFW_KEY_HOME) {
            moveCursorTo(0);
            return true;
        }
        if (!Screen.hasShiftDown() && keyCode == GLFWKey.GLFW_KEY_END) {
            moveCursorTo(value.length());
            return true;
        }

        // 处理删除
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

        // 处理其他按键
        if (Screen.hasShiftDown() && (keyCode == GLFWKey.GLFW_KEY_HOME || keyCode == GLFWKey.GLFW_KEY_END)) {
            String value = this.value;
            int totalWidth = renderCoordinate != null ? (int) renderCoordinate.width() - marginLeft - marginRight : 0;
            int innerWidth = getTextAreaWidth(totalWidth);
            if (innerWidth > 0 && this.font.width(value) > innerWidth) {
                if (keyCode == GLFWKey.GLFW_KEY_HOME) {
                    this.displayPos = 0;
                } else {
                    String tail = this.font.plainSubstrByWidth(value, innerWidth, true);
                    this.displayPos = Math.max(0, value.length() - tail.length());
                }
            } else {
                this.displayPos = 0;
            }
        } else {
            this.updateDisplayPos();
        }

        return false;
    }

    @Override
    protected boolean onCharTyped(char codePoint, int modifiers) {
        if (!focused() || !enabled || !editable) {
            return false;
        }

        if (value.length() >= maxLength && getHighlighted().isEmpty()) {
            return true;
        }

        insertText(String.valueOf(codePoint));
        return true;
    }

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
        if (this.displayPos > valueLength) {
            this.displayPos = valueLength;
        }
        if (this.highlightPos > valueLength) {
            this.highlightPos = valueLength;
        }
        if (this.cursorPosition > valueLength) {
            this.cursorPosition = valueLength;
        }
        this.updateDisplayPos();
        if (onTextChanged != null) {
            onTextChanged.accept(value);
        }
    }

    /**
     * 设置光标位置
     */
    public void setCursorPosition(int pos) {
        int newPos = MathHelper.clamp(pos, 0, value.length());
        if (this.cursorPosition != newPos) {
            // 移动光标时重置闪烁计时，使光标立即显示
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
        int newPos = MathHelper.clamp(this.cursorPosition + num, 0, value.length());
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
        // 跳过空格
        while (pos > 0 && Character.isWhitespace(value.charAt(pos - 1))) {
            pos--;
        }
        // 跳过单词字符
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
        // 跳过单词字符
        while (pos < value.length() && !Character.isWhitespace(value.charAt(pos))) {
            pos++;
        }
        // 跳过空格
        while (pos < value.length() && Character.isWhitespace(value.charAt(pos))) {
            pos++;
        }
        moveCursorTo(pos);
    }

    /**
     * 更新高亮位置
     */
    private void updateHighlightPos(int pos) {
        String value = this.value;
        int valueLength = value.length();
        this.highlightPos = MathHelper.clamp(pos, 0, valueLength);
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

        // 光标移动方向
        boolean movingRight = cursorPos > this.lastCursorPos;
        boolean movingLeft = cursorPos < this.lastCursorPos;
        if (cursorPos != this.lastCursorPos) {
            this.lastCursorPos = cursorPos;
        }

        // 从显示位置开始的所有剩余文本
        String remainingText = value.substring(this.displayPos);
        // 可见文本
        String visibleText = this.font.plainSubstrByWidth(remainingText, innerWidth);
        int visibleEnd = this.displayPos + visibleText.length();

        // 是否存在左右未显示的文本
        boolean hasLeftHidden = this.displayPos > 0;
        boolean hasRightHidden = visibleEnd < valueLength;

        // 计算光标在可见文本中的字符索引
        int cursorInVisible = cursorPos - this.displayPos;

        // 保证光标可见
        if (cursorPos < this.displayPos) {
            // 光标在显示位置之前，向左滚动
            String beforeCursor = value.substring(0, cursorPos);
            String reverseText = this.font.plainSubstrByWidth(beforeCursor, innerWidth, true);
            this.displayPos = Math.max(0, cursorPos - reverseText.length());
        } else if (cursorPos > visibleEnd) {
            // 光标超出可见范围，向右滚动
            String beforeCursor = value.substring(0, cursorPos);
            String reverseText = this.font.plainSubstrByWidth(beforeCursor, innerWidth, true);
            this.displayPos = Math.max(0, cursorPos - reverseText.length());
        } else {
            // 根据光标位置与lastCursorPos判断方向
            int lenVisible = visibleText.length();
            if (movingRight && hasRightHidden && lenVisible > 0) {
                int secondLastIndex = Math.max(0, lenVisible - 2);
                if (cursorInVisible >= secondLastIndex) {
                    // 使右侧文本缓慢滚动
                    this.displayPos = Math.min(this.displayPos + 1, valueLength);
                }
            } else if (movingLeft && hasLeftHidden && lenVisible > 0) {
                int secondIndex = 1;
                if (cursorInVisible <= secondIndex) {
                    // 使左侧文本缓慢滚动
                    this.displayPos = Math.max(this.displayPos - 1, 0);
                }
            }
        }

        // 检查高亮位置
        if (this.highlightPos != cursorPos) {
            // 重新计算可见文本
            visibleText = this.font.plainSubstrByWidth(value.substring(this.displayPos), innerWidth);
            visibleEnd = visibleText.length() + this.displayPos;

            // 确保调整后光标仍然可见
            boolean cursorVisible = cursorInVisible >= 0 && cursorInVisible <= visibleText.length();

            if (cursorVisible) {
                // 尝试让高亮位置可见
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
                    // 高亮位置超出可见范围
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

        // 确保在有效范围内
        this.displayPos = MathHelper.clamp(this.displayPos, 0, valueLength);
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
        int cursorPos = this.cursorPosition;
        int start = Math.min(cursorPos, this.highlightPos);
        int end = Math.max(cursorPos, this.highlightPos);
        boolean hasSelection = start != end;

        // 删除选中的文本
        if (hasSelection) {
            // 保存历史
            this.saveToHistory();

            // 删除选中的文本并插入新文本
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
            // 确保光标可见（智能滚动）
        } else {
            // 没有选中文本，正常插入
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
            // 确保光标可见
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
            // 向后删除
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
            // 向前删除
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
        // 确保光标可见
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

        // 删除字符
        String oldValue = this.value;
        this.saveToHistory();
        if (num < 0) {
            // 向后删除
            int start = Math.max(0, cursorPosition + num);
            value(value.substring(0, start) + value.substring(cursorPosition));
            setCursorPosition(start);
        } else {
            // 向前删除
            int end = Math.min(value.length(), cursorPosition + num);
            value(value.substring(0, cursorPosition) + value.substring(end));
        }
        if (!this.value.equals(oldValue)) {
            this.error = false;
        }
        // 重置高亮位置并更新显示位置
        this.highlightPos = this.cursorPosition;
        this.lastCursorPos = this.cursorPosition;
        // 确保光标可见
        this.updateDisplayPos();
    }

    /**
     * 获取选中的文本
     */
    @MethodsReturnNonnullByDefault
    public String getHighlighted() {
        int start = Math.min(this.cursorPosition, this.highlightPos);
        int end = Math.max(this.cursorPosition, this.highlightPos);
        String value = this.value;
        if (start >= 0 && end <= value.length() && start < end) {
            return value.substring(start, end);
        }
        return "";
    }

    /**
     * 检查是否可以消费输入
     */
    public boolean canConsumeInput() {
        return this.visible && this.focused() && this.editable;
    }


    /**
     * 保存当前状态到历史记录
     */
    private void saveToHistory() {
        String currentValue = this.value;
        if (!this.undoHistory.isEmpty() && this.undoHistory.peekLast().equals(currentValue)) {
            return;
        }

        // 添加到撤销历史
        this.undoHistory.offerLast(currentValue);
        if (this.undoHistory.size() > MAX_HISTORY_SIZE) {
            this.undoHistory.pollFirst();
        }

        // 清空重做历史
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

        // 从撤销历史中恢复上一个值
        String previousValue = this.undoHistory.pollLast();
        if (previousValue != null) {
            value(previousValue);
            this.error = false;
            // 重置光标和高亮位置
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
        // 将当前值添加到撤销历史
        this.undoHistory.offerLast(currentValue);
        if (this.undoHistory.size() > MAX_HISTORY_SIZE) {
            this.undoHistory.pollFirst();
        }

        // 从重做历史中恢复下一个值
        String nextValue = this.redoHistory.pollLast();
        if (nextValue != null) {
            value(nextValue);
            this.error = false;
            // 重置光标和高亮位置
            int cursorPos = Math.min(this.cursorPosition, nextValue.length());
            setCursorPosition(cursorPos);
            this.highlightPos = cursorPos;
            this.updateDisplayPos();
        }
    }

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
