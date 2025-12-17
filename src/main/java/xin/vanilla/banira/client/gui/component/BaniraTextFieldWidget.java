package xin.vanilla.banira.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 自定义样式的文本输入框
 */
public class BaniraTextFieldWidget extends TextFieldWidget {
    private static final int DEFAULT_BG_COLOR = 0x88000000;
    private static final int ERROR_BG_COLOR = 0x44FF0000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFF707070;

    private final int bgColor;
    private String hint;
    private final FontRenderer font;
    private boolean hasError = false;

    public BaniraTextFieldWidget(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
        this(font, x, y, width, height, message, DEFAULT_BG_COLOR);
    }

    public BaniraTextFieldWidget(FontRenderer font, int x, int y, int width, int height, ITextComponent message,
                                 int bgColor) {
        super(font, x, y, width, height, message);
        this.bgColor = bgColor;
        this.font = font;
        this.setTextColor(TEXT_COLOR);
        super.setBordered(false);
    }

    /**
     * 设置提示文本
     */
    public BaniraTextFieldWidget hint(@Nullable String hint) {
        this.hint = hint;
        return this;
    }

    /**
     * 设置是否有错误
     */
    public BaniraTextFieldWidget hasError(boolean hasError) {
        this.hasError = hasError;
        return this;
    }

    /**
     * 获取是否有错误
     */
    public boolean hasError() {
        return this.hasError;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void renderButton(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        int currentBgColor = this.hasError ? ERROR_BG_COLOR : this.bgColor;
        AbstractGuiUtils.fill(stack, this.x, this.y, this.width, this.height, currentBgColor, 1);

        // 使光标与内容居中
        stack.pushPose();
        stack.translate(2, 2, 0);
        super.renderButton(stack, mouseX, mouseY, partialTicks);
        stack.popPose();

        // 绘制提示文本
        if (StringUtils.isNotNullOrEmpty(this.hint) && this.getValue().isEmpty() && !this.isFocused()) {
            int hintX = this.x + 4;
            int hintY = this.y + (this.height - 8) / 2;
            this.font.draw(stack, this.hint, hintX, hintY, HINT_COLOR);
        }
    }
}
