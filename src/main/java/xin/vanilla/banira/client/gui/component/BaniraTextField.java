package xin.vanilla.banira.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * 自定义样式的文本输入框
 */
@Accessors(chain = true, fluent = true)
public class BaniraTextField extends TextFieldWidget {
    private static final int DEFAULT_BG_COLOR = 0x88000000;
    private static final int ERROR_BG_COLOR = 0x44FF0000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int TEXT_COLOR_UNEDITABLE = 0xFF707070;
    private static final int HINT_COLOR = 0xFF707070;
    private static final int CURSOR_COLOR = 0xFFE0E0E0;

    private static long LAST_CLICK_TIME = 0L;

    // 内边距
    private static final int PADDING_LEFT = 5;
    private static final int PADDING_RIGHT = 5;
    private static final int PADDING_TOP = 2;
    private static final int PADDING_BOTTOM = 2;

    private final int bgColor;
    @Setter
    private String hint;
    private final FontRenderer font;
    @Setter
    @Getter
    private boolean error = false;

    // 文本格式化器
    private BiFunction<String, Integer, IReorderingProcessor> formatter = (text, pos) ->
            IReorderingProcessor.forward(text, Style.EMPTY);

    /**
     * 显示位置
     */
    private int displayPos = 0;
    /**
     * 高亮位置
     */
    private int highlightPos = 0;
    /**
     * 文本颜色
     */
    private int textColor = TEXT_COLOR;
    /**
     * 是否可编辑
     */
    private boolean isEditable = true;
    /**
     * 最大长度
     */
    private int maxLength = 32;
    /**
     * 是否按住 Shift
     */
    private boolean shiftPressed = false;
    /**
     * 上一次的光标位置
     */
    private int lastCursorPos = 0;

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

    private static final Set<WeakReference<BaniraTextField>> INSTANCES = new HashSet<>();

    public BaniraTextField(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
        this(font, x, y, width, height, message, DEFAULT_BG_COLOR);
    }

    public BaniraTextField(FontRenderer font, int x, int y, int width, int height, ITextComponent message,
                           int bgColor) {
        super(font, x, y, width, height, message);
        this.bgColor = bgColor;
        this.font = font;
        // 初始化高亮位置和光标位置
        this.highlightPos = this.getCursorPosition();
        this.lastCursorPos = this.getCursorPosition();
        // 注册到实例集合
        synchronized (INSTANCES) {
            INSTANCES.add(new WeakReference<>(this));
            // 清理已回收的弱引用
            INSTANCES.removeIf(ref -> ref.get() == null);
        }
    }

    /**
     * 设置文本格式化器
     */
    @Override
    @ParametersAreNonnullByDefault
    public void setFormatter(BiFunction<String, Integer, IReorderingProcessor> formatter) {
        super.setFormatter(formatter);
        this.formatter = formatter;
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        this.textColor = color;
    }

    @Override
    public void setMaxLength(int maxLength) {
        super.setMaxLength(maxLength);
        this.maxLength = maxLength;
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        this.isEditable = editable;
    }

    @Override
    public void setCursorPosition(int pos) {
        super.setCursorPosition(pos);
        if (!this.shiftPressed) {
            this.updateHighlightPos(this.getCursorPosition());
        }
        this.updateDisplayPos();
    }

    @Override
    public void moveCursorTo(int pos) {
        super.moveCursorTo(pos);
        if (!this.shiftPressed) {
            this.updateHighlightPos(this.getCursorPosition());
        }
        this.updateDisplayPos();
    }

    @Override
    public void moveCursor(int num) {
        super.moveCursor(num);
        if (!this.shiftPressed) {
            this.updateHighlightPos(this.getCursorPosition());
        }
        // 更新显示位置
        this.updateDisplayPos();
    }

    /**
     * 更新高亮位置
     */
    private void updateHighlightPos(int pos) {
        String value = this.getValue();
        int valueLength = value.length();
        this.highlightPos = MathHelper.clamp(pos, 0, valueLength);
        this.updateDisplayPos();
    }

    /**
     * 更新显示位置
     */
    private void updateDisplayPos() {
        String value = this.getValue();
        int valueLength = value.length();
        int cursorPos = this.getCursorPosition();

        // 确保显示位置不超出文本长度
        if (this.displayPos > valueLength) {
            this.displayPos = valueLength;
        }

        int innerWidth = this.getInnerWidth();
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

        // 保证光标可见，再做细致滚动控制
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
                    // 使右侧文本缓慢滚入
                    this.displayPos = Math.min(this.displayPos + 1, valueLength);
                }
            } else if (movingLeft && hasLeftHidden && lenVisible > 0) {
                int secondIndex = 1;
                if (cursorInVisible <= secondIndex) {
                    // 使左侧文本缓慢滚入
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
                    // 高亮位置在显示位置之前
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

    /**
     * 获取内部宽度
     */
    @Override
    public int getInnerWidth() {
        return this.width - PADDING_LEFT - PADDING_RIGHT;
    }

    @Override
    protected void onFocusedChanged(boolean focused) {
        super.onFocusedChanged(focused);
        if (!focused) {
            this.highlightPos = this.getCursorPosition();
            this.shiftPressed = false;
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void renderButton(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        if (!this.isVisible()) {
            return;
        }

        this.updateDisplayPos();

        // 绘制背景
        int currentBgColor = this.error ? ERROR_BG_COLOR : this.bgColor;
        AbstractGuiUtils.fill(stack, this.x, this.y, this.width, this.height, currentBgColor, 1);

        // 获取文本和状态
        String value = this.getValue();
        int currentTextColor = this.textColor;
        if (!this.isEditable) {
            currentTextColor = TEXT_COLOR_UNEDITABLE;
        }
        int innerWidth = this.getInnerWidth();

        int cursorPos = this.getCursorPosition();
        int highlightPos = this.highlightPos;
        int displayPos = this.displayPos;

        // 获取可见文本
        String visibleText = this.font.plainSubstrByWidth(value.substring(displayPos), innerWidth);

        // 是否存在左右未显示文本
        boolean hasLeftHidden = displayPos > 0;
        boolean hasRightHidden = displayPos + visibleText.length() < value.length();

        // 计算光标在可见文本中的位置
        int cursorInVisible = cursorPos - displayPos;
        boolean cursorVisible = cursorInVisible >= 0 && cursorInVisible <= visibleText.length();
        // 光标缓慢闪烁
        boolean shouldShowCursor = this.isFocused() && ((System.currentTimeMillis() - LAST_CLICK_TIME) / 750) % 2 == 0 && cursorVisible;

        // 计算文本绘制位置
        int textX = this.x + PADDING_LEFT;
        int textY = this.y + PADDING_TOP + (this.height - PADDING_TOP - PADDING_BOTTOM - 8) / 2;

        // 绘制溢出内容标记
        int dotColor = currentTextColor;
        int centerY = textY + 4;
        if (hasLeftHidden) {
            int dotX = this.x + 2;
            AbstractGuiUtils.drawPixel(stack, dotX, centerY - 1, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX - 1, centerY, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX, centerY + 1, dotColor);
        }
        if (hasRightHidden) {
            int dotX = this.x + this.width - 3;
            AbstractGuiUtils.drawPixel(stack, dotX, centerY - 1, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX + 1, centerY, dotColor);
            AbstractGuiUtils.drawPixel(stack, dotX, centerY + 1, dotColor);
        }

        // 光标之前的文本
        int textDrawX = textX;
        if (!visibleText.isEmpty()) {
            String beforeCursor = cursorVisible ? visibleText.substring(0, Math.min(cursorInVisible, visibleText.length())) : visibleText;
            if (!beforeCursor.isEmpty()) {
                textDrawX = this.font.drawShadow(stack, this.formatter.apply(beforeCursor, displayPos),
                        (float) textX, (float) textY, currentTextColor);
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
            this.font.drawShadow(stack, this.formatter.apply(afterCursor, cursorPos),
                    (float) textDrawX, (float) textY, currentTextColor);
        }

        // 提示文本
        if (StringUtils.isNotNullOrEmpty(this.hint) && value.isEmpty() && !this.isFocused()) {
            this.font.drawShadow(stack, this.hint, (float) textX, (float) textY, HINT_COLOR);
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
                int highlightX1 = textX + this.font.width(visibleText.substring(0, highlightStartInVisible));
                int highlightX2 = textX + this.font.width(visibleText.substring(0, highlightEndInVisible));
                this.renderHighlight(stack, highlightX1, textY - 1, highlightX2, textY + 9);
            }
        }

        // 绘制光标
        if (shouldShowCursor) {
            if (isAtEnd) {
                // 在文本末尾绘制下划线光标
                this.font.drawShadow(stack, "_", cursorX, textY, currentTextColor);
            } else {
                // 在文本中间绘制竖线光标
                AbstractGuiUtils.fill(stack, cursorX, textY - 1, 1, 9, CURSOR_COLOR);
            }
        }
    }

    /**
     * 绘制文本选择高亮
     */
    private void renderHighlight(MatrixStack stack, int x1, int y1, int x2, int y2) {
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
        int maxX = this.x + this.width;
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
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION);
        bufferbuilder.vertex((double) x1, (double) y2, 0.0D).endVertex();
        bufferbuilder.vertex((double) x2, (double) y2, 0.0D).endVertex();
        bufferbuilder.vertex((double) x2, (double) y1, 0.0D).endVertex();
        bufferbuilder.vertex((double) x1, (double) y1, 0.0D).endVertex();
        tessellator.end();
        RenderSystem.disableColorLogicOp();
        RenderSystem.enableTexture();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void setValue(String value) {
        String oldValue = this.getValue();
        super.setValue(value);
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
        // 更新显示位置
        this.updateDisplayPos();
    }

    @Override
    @ParametersAreNonnullByDefault
    public void insertText(String text) {
        if (!this.isEditable) {
            return;
        }

        String value = this.getValue();
        int cursorPos = this.getCursorPosition();
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

            super.setValue(newValue);
            this.error = false;
            int newCursorPos = Math.min(start + text.length(), newValue.length());
            super.setCursorPosition(newCursorPos);
            this.highlightPos = newCursorPos;
            this.lastCursorPos = newCursorPos;
            // 确保光标可见（智能滚动）
            this.updateDisplayPos();
        } else {
            // 没有选中文本，正常插入
            if (!text.isEmpty()) {
                this.saveToHistory();
                this.error = false;
            }
            super.insertText(text);
            cursorPos = this.getCursorPosition();
            this.highlightPos = cursorPos;
            this.lastCursorPos = cursorPos;
            // 确保光标可见
            this.updateDisplayPos();
        }
    }

    @Override
    public void deleteWords(int num) {
        if (!this.isEditable || this.getValue().isEmpty()) {
            return;
        }

        // 删除选中的文本
        if (this.highlightPos != this.getCursorPosition()) {
            this.saveToHistory();
            this.insertText("");
            return;
        }

        // 删除单词
        String oldValue = this.getValue();
        this.saveToHistory();
        super.deleteWords(num);
        if (!this.getValue().equals(oldValue)) {
            this.error = false;
        }
        // 重置高亮位置并更新显示位置
        int cursorPos = this.getCursorPosition();
        this.highlightPos = cursorPos;
        this.lastCursorPos = cursorPos;
        // 确保光标可见
        this.updateDisplayPos();
    }

    @Override
    public void deleteChars(int num) {
        if (!this.isEditable || this.getValue().isEmpty() || num == 0) {
            return;
        }

        // 接删除选中的文本
        if (this.highlightPos != this.getCursorPosition()) {
            this.saveToHistory();
            this.insertText("");
            return;
        }

        // 删除字符
        String oldValue = this.getValue();
        this.saveToHistory();
        super.deleteChars(num);
        if (!this.getValue().equals(oldValue)) {
            this.error = false;
        }
        // 重置高亮位置并更新显示位置
        int cursorPos = this.getCursorPosition();
        this.highlightPos = cursorPos;
        this.lastCursorPos = cursorPos;
        // 确保光标可见
        this.updateDisplayPos();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.canConsumeInput()) {
            return false;
        }

        this.shiftPressed = Screen.hasShiftDown();

        // 处理 全选
        if (Screen.isSelectAll(keyCode)) {
            String value = this.getValue();
            // 直接设置光标位置到末尾
            super.setCursorPosition(value.length());
            this.highlightPos = 0;
            return true;
        }

        // 处理 Ctrl+Z 撤销
        if (Screen.hasControlDown() && keyCode == GLFWKey.GLFW_KEY_Z && !Screen.hasShiftDown()) {
            this.undo();
            return true;
        }

        // 处理 Ctrl+Y 或 Ctrl+Shift+Z 重做
        if ((Screen.hasControlDown() && keyCode == GLFWKey.GLFW_KEY_Y) ||
                (Screen.hasControlDown() && Screen.hasShiftDown() && keyCode == GLFWKey.GLFW_KEY_Z)) {
            this.redo();
            return true;
        }

        // 处理 复制
        if (Screen.isCopy(keyCode)) {
            net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
            return true;
        }

        // 处理 粘贴
        if (Screen.isPaste(keyCode)) {
            if (this.isEditable) {
                this.saveToHistory();
                this.insertText(net.minecraft.client.Minecraft.getInstance().keyboardHandler.getClipboard());
            }
            return true;
        }

        // 处理 剪切
        if (Screen.isCut(keyCode)) {
            net.minecraft.client.Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
            if (this.isEditable) {
                this.saveToHistory();
                this.insertText("");
            }
            return true;
        }

        // 处理其他按键
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        if (result) {
            // 滚动文本至头/尾
            if (Screen.hasShiftDown() && (keyCode == GLFWKey.GLFW_KEY_HOME || keyCode == GLFWKey.GLFW_KEY_END)) {
                String value = this.getValue();
                int innerWidth = this.getInnerWidth();
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
        }
        return result;
    }

    /**
     * 获取选中的文本
     */
    @Override
    @MethodsReturnNonnullByDefault
    public String getHighlighted() {
        int start = Math.min(this.getCursorPosition(), this.highlightPos);
        int end = Math.max(this.getCursorPosition(), this.highlightPos);
        String value = this.getValue();
        if (start >= 0 && end <= value.length() && start < end) {
            return value.substring(start, end);
        }
        return "";
    }

    /**
     * 检查是否可以消费输入
     */
    public boolean canConsumeInput() {
        return this.isVisible() && this.isFocused() && this.isEditable;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isVisible()) {
            return false;
        }

        boolean isInBounds = mouseX >= (double) this.x && mouseX < (double) (this.x + this.width)
                && mouseY >= (double) this.y && mouseY < (double) (this.y + this.height);

        if (this.canLoseFocus()) {
            this.setFocused(isInBounds);
        }

        if (this.isFocused() && isInBounds && button == 0) {
            // 通知所有其他 BaniraTextFieldWidget 取消焦点
            LAST_CLICK_TIME = System.currentTimeMillis();
            notifyOtherInstancesToLoseFocus();

            // 计算点击位置对应的文本位置
            int clickX = MathHelper.floor(mouseX) - this.x - PADDING_LEFT;
            if (clickX < 0) {
                clickX = 0;
            }
            String visibleText = this.font.plainSubstrByWidth(
                    this.getValue().substring(this.displayPos),
                    this.getInnerWidth());
            int textPos = this.font.plainSubstrByWidth(visibleText, clickX).length() + this.displayPos;

            this.shiftPressed = Screen.hasShiftDown();

            // 移动光标到点击位置
            this.moveCursorTo(textPos);

            // 重置高亮位置
            if (!this.shiftPressed) {
                this.highlightPos = textPos;
            }

            return true;
        }

        return false;
    }

    /**
     * 检查是否可以失去焦点
     */
    private boolean canLoseFocus() {
        return true;
    }

    /**
     * 通知所有其他实例取消焦点
     */
    private void notifyOtherInstancesToLoseFocus() {
        synchronized (INSTANCES) {
            INSTANCES.removeIf(ref -> {
                BaniraTextField instance = ref.get();
                if (instance == null) {
                    return true;
                }
                if (instance != this && instance.isFocused()) {
                    instance.setFocused(false);
                }
                return false;
            });
        }
    }

    /**
     * 保存当前状态到历史记录
     */
    private void saveToHistory() {
        String currentValue = this.getValue();
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

        String currentValue = this.getValue();
        // 将当前值添加到重做历史
        this.redoHistory.offerLast(currentValue);
        if (this.redoHistory.size() > MAX_HISTORY_SIZE) {
            this.redoHistory.pollFirst();
        }

        // 从撤销历史中恢复上一个值
        String previousValue = this.undoHistory.pollLast();
        if (previousValue != null) {
            super.setValue(previousValue);
            this.error = false;
            // 重置光标和高亮位置
            int cursorPos = Math.min(this.getCursorPosition(), previousValue.length());
            super.setCursorPosition(cursorPos);
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

        String currentValue = this.getValue();
        // 将当前值添加到撤销历史
        this.undoHistory.offerLast(currentValue);
        if (this.undoHistory.size() > MAX_HISTORY_SIZE) {
            this.undoHistory.pollFirst();
        }

        // 从重做历史中恢复下一个值
        String nextValue = this.redoHistory.pollLast();
        if (nextValue != null) {
            super.setValue(nextValue);
            this.error = false;
            // 重置光标和高亮位置
            int cursorPos = Math.min(this.getCursorPosition(), nextValue.length());
            super.setCursorPosition(cursorPos);
            this.highlightPos = cursorPos;
            this.updateDisplayPos();
        }
    }
}
