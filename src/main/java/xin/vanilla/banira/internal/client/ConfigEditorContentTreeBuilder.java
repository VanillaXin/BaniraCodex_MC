package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.common.config.ConfigCategoryTitleTexts;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.data.Component;

import java.util.List;

/**
 * 将配置描述树转换为当前版本可用的折叠面板树。
 */
public final class ConfigEditorContentTreeBuilder {
    private final BaniraScreen screen;
    private final ConfigHolder holder;
    private final ConfigEditorRowFactory rowFactory;
    private final ConfigEditorState editorState;
    private final Runnable contentHeightChanged;
    private final int rowHeight;
    private final int rowGap;

    public ConfigEditorContentTreeBuilder(BaniraScreen screen, ConfigHolder holder, ConfigEditorRowFactory rowFactory,
                                          ConfigEditorState editorState, Runnable contentHeightChanged,
                                          int rowHeight, int rowGap) {
        this.screen = screen;
        this.holder = holder;
        this.rowFactory = rowFactory;
        this.editorState = editorState;
        this.contentHeightChanged = contentHeightChanged;
        this.rowHeight = rowHeight;
        this.rowGap = rowGap;
    }

    /**
     * 构建根面板；空配置也会返回一个可渲染的占位根。
     */
    public CollapsiblePanelWidget build(double contentWidth) {
        List<ConfigHolder.CategoryTreeNode> roots = holder.getCategoryTree();
        String rootTitle = holder.getConfigName();
        if (rootTitle == null || rootTitle.isEmpty()) {
            rootTitle = "General";
        }
        if (roots.isEmpty()) {
            CollapsiblePanelWidget empty = createPanel(contentWidth, rootTitle, true);
            empty.onExpandChanged(p -> contentHeightChanged.run());
            return empty;
        }

        CollapsiblePanelWidget rootPanel = createPanel(contentWidth, rootTitle, true);
        buildPanelContent(rootPanel, roots.get(0));
        rootPanel.refreshLayout();
        return rootPanel;
    }

    private void buildPanelContent(CollapsiblePanelWidget panel, ConfigHolder.CategoryTreeNode node) {
        double childWidth = panel.getContentWidth();
        for (ConfigEntryDescriptor desc : node.getEntries()) {
            ConfigEditorEntryWidget adapter = rowFactory.createEntryRow(desc, childWidth, rowHeight);
            if (adapter != null) {
                editorState.registerEntry(desc.getPath(), adapter);
                double h = adapter.getWidget().effectiveHeight() > 0 ? adapter.getWidget().effectiveHeight() : rowHeight;
                panel.addChildAuto(adapter.getWidget(), h);
            }
        }
        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
            CollapsiblePanelWidget childPanel = createPanel(childWidth,
                    ConfigCategoryTitleTexts.categoryTitleComponent(holder.getCategoryTitleSpec(child.getCategoryPath()),
                            rowFactory.configModId(), child.getDisplayName()),
                    false);
            buildPanelContent(childPanel, child);
            childPanel.refreshLayout();
            panel.addCollapsibleChild(childPanel);
        }
    }

    private CollapsiblePanelWidget createPanel(double width, String title, boolean expanded) {
        CollapsiblePanelWidget panel = CollapsiblePanelWidget.createAutoHeight(screen, 0, 0, width);
        panel.text(title).expanded(expanded);
        configurePanel(panel);
        return panel;
    }

    private CollapsiblePanelWidget createPanel(double width, Component title, boolean expanded) {
        CollapsiblePanelWidget panel = CollapsiblePanelWidget.createAutoHeight(screen, 0, 0, width);
        panel.text(title).expanded(expanded);
        configurePanel(panel);
        return panel;
    }

    private void configurePanel(CollapsiblePanelWidget panel) {
        panel.contentGap(rowGap);
        panel.headerHeight(rowHeight);
        panel.onExpandChanged(p -> contentHeightChanged.run());
    }
}
