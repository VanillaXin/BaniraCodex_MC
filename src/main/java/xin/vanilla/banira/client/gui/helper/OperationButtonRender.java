package xin.vanilla.banira.client.gui.helper;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import xin.vanilla.banira.client.gui.component.OperationButton;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

/**
 * 按钮渲染辅助类
 */
public final class OperationButtonRender {
    private OperationButtonRender() {
    }

    /**
     * 渲染操作按钮背景
     */
    public static void renderOperationButtonBackground(OperationButton.RenderContext context) {
        int lineColor = context.button.hovered()
                ? LayoutConfig.Colors.BUTTON_BORDER_HOVER
                : LayoutConfig.Colors.BUTTON_BORDER;

        AbstractGuiUtils.drawRoundedRect(
                context.matrixStack,
                (int) context.button.x(),
                (int) context.button.y(),
                (int) context.button.width(),
                (int) context.button.height(),
                LayoutConfig.Colors.BUTTON_BACKGROUND,
                2
        );
        AbstractGuiUtils.drawRoundedRectOutLineRough(
                context.matrixStack,
                (int) context.button.x(),
                (int) context.button.y(),
                (int) context.button.width(),
                (int) context.button.height(),
                1,
                lineColor,
                2
        );
    }

    /**
     * 渲染操作按钮图标
     */
    public static void renderOperationButtonIcon(OperationButton.RenderContext context, ItemRenderer itemRenderer, ItemStack icon) {
        itemRenderer.renderGuiItem(
                icon,
                (int) context.button.x() + 2,
                (int) context.button.y() + 2
        );
    }

    /**
     * 设置操作按钮工具提示
     */
    public static void setOperationButtonTooltip(OperationButton.RenderContext context, Text tooltip) {
        context.button.tooltip(tooltip);
    }
}
