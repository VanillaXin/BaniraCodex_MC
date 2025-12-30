package xin.vanilla.banira.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * 自定义样式的按钮
 */
public class BaniraButton extends Button {
    private static final int DEFAULT_BG_COLOR = 0xFF404040;
    private static final int HOVER_BG_COLOR = 0xFF505050;
    private static final int PRESSED_BG_COLOR = 0xFF303030;
    private static final int BORDER_COLOR = 0xFF5A5A5A;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private final int bgColor;
    private final int hoverBgColor;
    private final int pressedBgColor;
    private final int borderColor;

    // 鼠标是否在按钮上按下
    private boolean mousePressedOnButton = false;

    public BaniraButton(int x, int y, int width, int height, ITextComponent message, IPressable onPress) {
        this(x, y, width, height, message, onPress, DEFAULT_BG_COLOR, HOVER_BG_COLOR, PRESSED_BG_COLOR, BORDER_COLOR);
    }

    public BaniraButton(int x, int y, int width, int height, ITextComponent message, IPressable onPress,
                        int bgColor, int hoverBgColor, int pressedBgColor, int borderColor) {
        super(x, y, width, height, message, onPress);
        this.bgColor = bgColor;
        this.hoverBgColor = hoverBgColor;
        this.pressedBgColor = pressedBgColor;
        this.borderColor = borderColor;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void renderButton(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        int currentBgColor;
        if (!this.active) {
            currentBgColor = (this.bgColor & 0xFFFFFF) | 0x7F000000;
        } else if (this.mousePressedOnButton && this.isHovered()) {
            currentBgColor = this.pressedBgColor;
        } else if (this.isHovered()) {
            currentBgColor = this.hoverBgColor;
        } else {
            currentBgColor = this.bgColor;
        }

        // 绘制背景
        AbstractGuiUtils.drawRoundedRect(stack, this.x, this.y, this.width, this.height, currentBgColor, 2);

        // 绘制边框
        AbstractGuiUtils.drawRoundedRectOutLineRough(stack, this.x, this.y, this.width, this.height, 1, this.borderColor, 2);

        // 绘制文本
        FontRenderer font = Minecraft.getInstance().font;
        int textColor = this.active ? TEXT_COLOR : 0xFFA0A0A0;
        int textWidth = font.width(this.getMessage());
        int textX = this.x + (this.width - textWidth) / 2;
        int textY = this.y + (this.height - 8) / 2;

        font.draw(stack, this.getMessage(), textX, textY, textColor);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
            this.mousePressedOnButton = true;
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }
        if (button == 0) {
            this.mousePressedOnButton = false;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.mousePressedOnButton) {
            this.mousePressedOnButton = false;
            if (this.active && this.visible && this.isMouseOver(mouseX, mouseY)) {
                this.onPress();
                return true;
            }
        }
        return false;
    }
}
