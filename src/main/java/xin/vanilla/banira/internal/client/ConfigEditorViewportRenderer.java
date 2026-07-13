package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;

import java.util.function.Predicate;

/**
 * 配置编辑器滚动视口渲染器，集中处理 scissor 与 overlay 渲染顺序。
 */
public final class ConfigEditorViewportRenderer {
    private ConfigEditorViewportRenderer() {
    }

    /**
     * 在裁剪区域内渲染配置树和滚动条。
     */
    public static void renderScrolledContent(PoseStack stack, float partialTicks,
                                             CollapsiblePanelWidget contentRootPanel, ScrollbarWidget scrollbar,
                                             int contentLeft, int listTop, int contentTotalW, int listAreaHeight) {
        AbstractGuiUtils.enableScissor(contentLeft, listTop, contentTotalW, Math.max(1, listAreaHeight));
        renderWidget(contentRootPanel, stack, partialTicks);
        renderWidget(scrollbar, stack, partialTicks);
        AbstractGuiUtils.disableScissor();
    }

    /**
     * 在 scissor 关闭后渲染顶层 overlay 控件，避免 tooltip/popup 被配置列表裁剪。
     */
    public static void renderOverlayWidgets(Iterable<IWidget> widgets, PoseStack stack, float partialTicks,
                                            Predicate<IWidget> reservedWidget) {
        for (IWidget widget : widgets) {
            if (reservedWidget.test(widget) || widget.parent() != null || !widget.visible()) {
                continue;
            }
            renderWidget(widget, stack, partialTicks);
        }
    }

    private static void renderWidget(IWidget widget, PoseStack stack, float partialTicks) {
        if (widget != null && widget.visible()) {
            if (widget.enabled() && widget.needsUpdate()) {
                widget.update();
            }
            widget.render(stack, partialTicks);
        }
    }
}
