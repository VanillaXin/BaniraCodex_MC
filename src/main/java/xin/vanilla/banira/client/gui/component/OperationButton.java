package xin.vanilla.banira.client.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.KeyEventManager;
import xin.vanilla.banira.client.util.MouseHelper;
import xin.vanilla.banira.common.util.StringUtils;

import java.util.function.Consumer;

/**
 * 自定义渲染按钮
 */
@Data
@Accessors(chain = true, fluent = true)
@OnlyIn(Dist.CLIENT)
public class OperationButton {
    /**
     * 渲染上下文
     */
    public static class RenderContext {
        public final MatrixStack matrixStack;
        public final KeyEventManager keyManager;
        public final MouseHelper mouseHelper;
        public final OperationButton button;

        public RenderContext(MatrixStack matrixStack, KeyEventManager keyManager, MouseHelper mouseHelper, OperationButton button) {
            this.matrixStack = matrixStack;
            this.keyManager = keyManager;
            this.mouseHelper = mouseHelper;
            this.button = button;
        }

        public double mouseX() {
            return this.mouseHelper.mouseX();
        }

        public double mouseY() {
            return this.mouseHelper.mouseY();
        }
    }

    /**
     * 按钮ID
     */
    private String id = "";

    /**
     * 操作标识
     */
    private int operation;

    /**
     * 按钮位置和尺寸
     */
    private double x, y, width, height;

    /**
     * 按钮状态
     */
    private boolean pressed = false;
    private boolean hovered = false;

    /**
     * 自定义渲染函数
     */
    private Consumer<RenderContext> customRenderFunction;

    /**
     * 鼠标提示
     */
    private Text tooltip;

    /**
     * 提示文字是否仅按下按键时显示
     */
    private String keyNames;

    public OperationButton(int operation, Consumer<RenderContext> customRenderFunction) {
        this.operation = operation;
        this.customRenderFunction = customRenderFunction;
    }

    /**
     * 判断鼠标是否在按钮内
     */
    public boolean isMouseOverEx(MouseHelper mouseHelper) {
        return mouseHelper.mouseX() >= this.x && mouseHelper.mouseX() <= this.x + this.width && mouseHelper.mouseY() >= this.y && mouseHelper.mouseY() <= this.y + this.height;
    }

    /**
     * 判断鼠标是否在按钮内
     */
    public boolean isMouseOverEx(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    /**
     * 绘制按钮
     */
    public void render(MatrixStack matrixStack, KeyEventManager keyManager, MouseHelper mouseHelper) {
        if (customRenderFunction != null) {
            customRenderFunction.accept(new RenderContext(matrixStack, keyManager, mouseHelper, this));
        }
        renderPopup(matrixStack, null, keyManager, mouseHelper);
    }

    /**
     * 绘制弹出层提示
     */
    public void renderPopup(MatrixStack stack, FontRenderer font, KeyEventManager keyManager, MouseHelper mouseHelper) {
        if (StringUtils.isNullOrEmptyEx(this.keyNames) || keyManager.isKeyPressed(this.keyNames)) {
            if (this.hovered() && tooltip != null && StringUtils.isNotNullOrEmpty(tooltip.content())) {
                if (Minecraft.getInstance().screen != null) {
                    if (font == null) {
                        font = Minecraft.getInstance().font;
                    }
                    AbstractGuiUtils.drawPopupMessageWithSeason(FontDrawArgs.ofPopo(tooltip.stack(stack).font(font))
                            .x(mouseHelper.mouseX())
                            .y(mouseHelper.mouseY())
                            .padding(0)
                    );
                }
            }
        }
    }
}
