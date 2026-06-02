package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.config.ConfigCategoryTitleTexts;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.internal.client.*;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 配置编辑界面，支持可视化编辑 Banira 配置描述符。
 * <ul>
 *   <li>单击「同步至服务端」仅发送本会话内改动过的配置项；长按发送全部项。</li>
 *   <li>可同步类配置下，长按「保存」可从服务端拉取全量快照并刷新界面。</li>
 * </ul>
 */
public class ConfigEditorScreen extends BaniraScreen {

    private static final int CARD_MARGIN = 10;
    private static final int CARD_INNER = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_GAP = 2;

    private final ConfigHolder holder;
    private final Args args;
    private final ConfigEditorState editorState;
    private final ConfigEditorRowFactory rowFactory;
    private final ConfigEditorActionBar actionBar;

    private CollapsiblePanelWidget contentRootPanel;
    private ScrollbarWidget scrollbar;
    private double scrollOffset = 0;
    private int contentHeight = 0;
    private int cardX;
    private int cardY;
    private int cardW;
    private int cardH;
    private int listTop;
    private int listAreaHeight;
    private int maxListHeight;
    private int contentLeft;
    private int contentW;
    private int contentTotalW;

    public ConfigEditorScreen(ConfigHolder holder, Args args) {
        super(BaniraComponent.get().transClientAuto("config_editor_title").toVanilla());
        this.holder = holder;
        this.args = args != null ? args : new Args();
        this.editorState = new ConfigEditorState(holder);
        this.rowFactory = new ConfigEditorRowFactory(this, holder, editorState, this::syncContentHeight);
        this.actionBar = new ConfigEditorActionBar(this, holder, this::saveConfig, this::fetchConfigFromServer,
                this::syncToServer, this::syncToServerFull, this::onClose);
        previousScreen(args != null ? args.parentScreen() : null);
        BaniraScreen.inheritThemeAndSeason(this, args != null ? args.parentScreen() : null, args != null ? args.theme() : null, args != null ? args.season() : null);
    }

    public static void open(ConfigHolder holder, @Nullable Screen parent) {
        if (BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient()) {
            Minecraft.getInstance().setScreen(new ConfigEditorScreen(holder, new Args().parentScreen(parent)));
        }
    }

    @Override
    protected void initWidgets() {
        int w = width;
        int h = height;
        cardX = CARD_MARGIN;
        cardY = CARD_MARGIN;
        cardW = w - CARD_MARGIN * 2;
        cardH = h - CARD_MARGIN * 2;
        contentLeft = cardX + CARD_INNER;
        contentW = cardW - CARD_INNER * 2 - SCROLL_WIDTH - SCROLL_GAP;
        contentTotalW = contentW + SCROLL_GAP + SCROLL_WIDTH;
        listTop = cardY + CARD_INNER;
        editorState.clearEntries();
        actionBar.rebuildButtons();

        contentRootPanel = buildContentPanel();
        contentHeight = (int) contentRootPanel.height();
        addWidget(contentRootPanel);

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL);
        scrollbar.minValue(0);
        scrollbar.onValueChanged(v -> {
            scrollOffset = v;
            updateWidgetPositions();
        });
        addWidget(scrollbar);

        for (ButtonWidget btn : actionBar.buttons()) {
            addWidget(btn);
        }

        updateLayout();
        updateWidgetPositions();
    }

    /**
     * 使用 CollapsiblePanelWidget 构建配置树
     */
    private CollapsiblePanelWidget buildContentPanel() {
        List<ConfigHolder.CategoryTreeNode> roots = holder.getCategoryTree();
        String rootTitle = holder.getConfigName();
        if (rootTitle == null || rootTitle.isEmpty()) {
            rootTitle = "General";
        }
        if (roots.isEmpty()) {
            CollapsiblePanelWidget empty = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentW);
            empty.text(rootTitle).expanded(true);
            empty.onExpandChanged(p -> syncContentHeight());
            return empty;
        }
        ConfigHolder.CategoryTreeNode rootNode = roots.get(0);
        CollapsiblePanelWidget rootPanel = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentW);
        rootPanel.text(rootTitle).expanded(true);
        rootPanel.contentGap(ROW_GAP);
        rootPanel.headerHeight(ROW_HEIGHT);
        rootPanel.onExpandChanged(p -> syncContentHeight());

        buildPanelContent(rootPanel, rootNode);
        rootPanel.refreshLayout();
        return rootPanel;
    }

    private void buildPanelContent(CollapsiblePanelWidget panel, ConfigHolder.CategoryTreeNode node) {
        double cw = panel.getContentWidth();
        for (ConfigEntryDescriptor desc : node.getEntries()) {
            ConfigEditorEntryWidget adapter = rowFactory.createEntryRow(desc, cw, ROW_HEIGHT);
            if (adapter != null) {
                editorState.registerEntry(desc.getPath(), adapter);
                double rowHeight = adapter.getWidget().effectiveHeight() > 0 ? adapter.getWidget().effectiveHeight() : ROW_HEIGHT;
                panel.addChildAuto(adapter.getWidget(), rowHeight);
            }
        }
        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
            CollapsiblePanelWidget childPanel = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, cw);
            childPanel.text(ConfigCategoryTitleTexts.categoryTitleComponent(holder.getCategoryTitleSpec(child.getCategoryPath()),
                    rowFactory.configModId(), child.getDisplayName())).expanded(false);
            childPanel.contentGap(ROW_GAP);
            childPanel.headerHeight(ROW_HEIGHT);
            childPanel.onExpandChanged(p -> syncContentHeight());
            buildPanelContent(childPanel, child);
            childPanel.refreshLayout();
            panel.addCollapsibleChild(childPanel);
        }
    }

    private void syncContentHeight() {
        if (contentRootPanel != null) {
            contentRootPanel.refreshLayout();
            contentHeight = (int) contentRootPanel.height();
            updateLayout();
            updateWidgetPositions();
        }
    }

    private void updateLayout() {
        maxListHeight = actionBar.maxScrollableHeight(cardH, CARD_INNER);

        if (contentHeight <= maxListHeight) {
            listAreaHeight = Math.max(1, contentHeight);
            scrollOffset = 0;
            scrollbar.maxValue(0);
            scrollbar.value(0);
            scrollbar.visible(false);
            scrollbar.scrollingCoordinates(new ArrayList<>());
        } else {
            listAreaHeight = maxListHeight;
            scrollbar.visible(true);
            scrollbar.bounds(new ScreenCoordinate(contentLeft + contentW + SCROLL_GAP, listTop, SCROLL_WIDTH, listAreaHeight));
            scrollbar.maxValue(Math.max(0, contentHeight - listAreaHeight));
            scrollbar.value(Math.min(scrollOffset, scrollbar.maxValue()));
            scrollOffset = scrollbar.value();
            scrollbar.visibleSize(listAreaHeight);
            scrollbar.scrollingCoordinates(new ArrayList<>());
            scrollbar.addScrollHoverArea(new ScreenCoordinate(contentLeft, listTop, contentTotalW, listAreaHeight));
        }

        actionBar.layout(cardX, cardY, cardW, cardH, CARD_INNER, b -> font.width(b.text().toString()));
    }

    private void updateWidgetPositions() {
        if (contentRootPanel != null) {
            contentRootPanel.bounds(new ScreenCoordinate(contentLeft, listTop - (int) scrollOffset, contentW, contentHeight));
        }
    }

    private void saveConfig() {
        editorState.collectModifiedFromWidgets();
        if (editorState.hasInvalidEntryWidgets()) {
            ConfigEditorNotifier.show("config_editor_validation_failed", 3000);
            return;
        }
        editorState.applyModifiedToHolder();
        editorState.clearModifiedValues();
        try {
            holder.save();
            ConfigEditorNotifier.show("config_editor_save_success", 2000);
            // if (previousScreen() != null) {
            //     onClose();
            // }
        } catch (Exception ex) {
            ConfigEditorNotifier.show("config_editor_save_failed", 4000, ex.getMessage());
        }
    }

    private void syncToServer() {
        if (editorState.hasInvalidEntryWidgets()) {
            ConfigEditorNotifier.show("config_editor_validation_failed", 3000);
            return;
        }
        Map<String, Object> syncPayload = editorState.collectTouchedPathsForSync();
        if (syncPayload.isEmpty()) {
            ConfigEditorNotifier.show("config_editor_sync_nothing", 2500);
            return;
        }
        Map<String, String> toSync = ConfigEditorSyncService.encodePayload(syncPayload);
        try {
            ConfigEditorSyncService.sendSync(holder, toSync);
            editorState.clearPendingChanges();
            ConfigEditorSyncService.applyEncodedValues(holder, toSync);
        } catch (Exception ex) {
            ConfigEditorNotifier.show("config_editor_sync_failed", 4000, ex.getMessage() != null ? ex.getMessage() : "");
        }
    }

    /**
     * 长按「同步至服务端」：上传当前界面中全部配置项（全量）。
     */
    private void syncToServerFull() {
        if (editorState.hasInvalidEntryWidgets()) {
            ConfigEditorNotifier.show("config_editor_validation_failed", 3000);
            return;
        }
        if (!ConfigEditorSyncService.hasServerConnection()) {
            ConfigEditorNotifier.show("config_editor_sync_not_connected", 3500);
            return;
        }
        Map<String, Object> syncPayload = editorState.collectAllEntryValuesForSync();
        if (syncPayload.isEmpty()) {
            ConfigEditorNotifier.show("config_editor_sync_nothing", 2500);
            return;
        }
        Map<String, String> toSync = ConfigEditorSyncService.encodePayload(syncPayload);
        try {
            ConfigEditorSyncService.sendSync(holder, toSync);
            editorState.clearPendingChanges();
            ConfigEditorSyncService.applyEncodedValues(holder, toSync);
        } catch (Exception ex) {
            ConfigEditorNotifier.show("config_editor_sync_full_failed", 4000, ex.getMessage() != null ? ex.getMessage() : "");
        }
    }

    /**
     * 长按「保存」：向服务端请求当前配置的全量快照并刷新本界面
     */
    private void fetchConfigFromServer() {
        if (!holder.canSyncToServer()) {
            return;
        }
        if (!ConfigEditorSyncService.hasServerConnection()) {
            ConfigEditorNotifier.show("config_editor_fetch_not_connected", 3500);
            return;
        }
        try {
            ConfigEditorSyncService.requestSnapshot(holder);
        } catch (Exception ex) {
            ConfigEditorNotifier.show("config_editor_fetch_send_failed", 4000, ex.getMessage() != null ? ex.getMessage() : "");
        }
    }

    /**
     * 在收到 {@link xin.vanilla.banira.common.network.packet.ConfigSnapshotToClient} 后，若本界面正在编辑对应配置，则用 {@link ConfigHolder} 刷新控件显示。
     */
    public void refreshUIFromHolderAfterRemoteFetch(String configName) {
        if (!holder.getConfigName().equals(configName)) {
            return;
        }
        editorState.refreshEntriesFromHolder(configName);
    }

    @Override
    protected void renderWidgets(MatrixStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        actionBar.renderChrome(stack, theme, cardX, cardY, cardW, cardH, CARD_INNER);

        AbstractGuiUtils.enableScissor(contentLeft, listTop, contentTotalW, Math.max(1, listAreaHeight));

        if (contentRootPanel != null && contentRootPanel.visible()) {
            if (contentRootPanel.enabled() && contentRootPanel.needsUpdate()) contentRootPanel.update();
            contentRootPanel.render(stack, partialTicks);
        }
        if (scrollbar != null && scrollbar.visible()) {
            if (scrollbar.enabled() && scrollbar.needsUpdate()) scrollbar.update();
            scrollbar.render(stack, partialTicks);
        }

        AbstractGuiUtils.disableScissor();

        actionBar.renderButtons(stack, partialTicks);

        // 渲染 overlay 控件，需在 scissor 关闭后渲染以免被裁剪
        for (IWidget widget : widgets()) {
            if (widget == contentRootPanel || widget == scrollbar || actionBar.contains(widget)) continue;
            if (widget.parent() != null || !widget.visible()) continue;
            if (widget.enabled() && widget.needsUpdate()) widget.update();
            widget.render(stack, partialTicks);
        }
    }

    @Override
    protected void onRender(MatrixStack stack, float partialTicks) {
        renderWidgets(stack, partialTicks);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && contentRootPanel != null && contentRootPanel.visible() && contentRootPanel.enabled()
                && contentRootPanel.isMouseInside(mouseX, mouseY)
                && contentRootPanel.handleMouseScroll(MouseScrollEvent.of(mouseX, mouseY, delta))) {
            return true;
        }
        if (super.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        if (scrollbar != null && delta != 0) {
            double newVal = scrollbar.value() - delta * 20;
            newVal = Math.max(scrollbar.minValue(), Math.min(scrollbar.maxValue(), newVal));
            scrollbar.value(newVal);
            scrollOffset = newVal;
            updateWidgetPositions();
            return true;
        }
        return false;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class Args {
        private Screen parentScreen;
        private BaniraColorConfig theme;
        private EnumSeason season;
    }
}
