package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.search.ConfigSearchQuery;
import xin.vanilla.banira.client.gui.search.ConfigSearchText;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.InputWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.config.ConfigEntryTooltipTexts;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.*;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
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
    private static final int SEARCH_HEIGHT = 18;
    private static final int SEARCH_GAP = 4;

    private final ConfigHolder holder;
    private final Args args;
    private final ConfigEditorState editorState;
    private final ConfigEditorRowFactory rowFactory;
    private final ConfigEditorContentTreeBuilder contentTreeBuilder;
    private final ConfigEditorActionBar actionBar;
    private final ConfigEditorViewportModel viewport;
    private final Map<String, Boolean> expandedBeforeSearch = new LinkedHashMap<>();

    private CollapsiblePanelWidget contentRootPanel;
    private InputWidget searchInput;
    private ScrollbarWidget scrollbar;
    private String searchText = "";
    private boolean applyingSearch;

    public ConfigEditorScreen(ConfigHolder holder, Args args) {
        super(BaniraComponent.get().transClientAuto("config_editor_title").toVanilla());
        this.holder = holder;
        this.args = args != null ? args : new Args();
        this.editorState = new ConfigEditorState(holder);
        this.rowFactory = new ConfigEditorRowFactory(this, holder, editorState, this::syncContentHeight);
        this.contentTreeBuilder = new ConfigEditorContentTreeBuilder(this, holder, rowFactory, editorState,
                this::syncContentHeight, ROW_HEIGHT, ROW_GAP);
        this.actionBar = new ConfigEditorActionBar(this, holder, this::saveConfig, this::fetchConfigFromServer,
                this::syncToServer, this::syncToServerFull, this::onClose);
        this.viewport = new ConfigEditorViewportModel(
                CARD_MARGIN, CARD_INNER, SCROLL_WIDTH, SCROLL_GAP, SEARCH_HEIGHT + SEARCH_GAP);
        previousScreen(args != null ? args.parentScreen() : null);
        BaniraScreen.inheritThemeAndSeason(this, args != null ? args.parentScreen() : null,
                args != null ? args.theme() : null, args != null ? args.season() : null);
    }

    public static void open(ConfigHolder holder, @Nullable Screen parent) {
        BaniraClientScreenService.openConfigEditor(holder, parent);
    }

    @Override
    protected void initWidgets() {
        viewport.resize(width, height);
        editorState.clearEntries();
        expandedBeforeSearch.clear();
        actionBar.rebuildButtons();

        searchInput = new InputWidget(this);
        searchInput.id("config_search");
        searchInput.text(BaniraComponent.get().transClientAuto("config_search_hint"));
        searchInput.value(searchText);
        searchInput.onTextChanged(this::applySearchFilter);
        addWidget(searchInput);

        contentRootPanel = contentTreeBuilder.build(viewport.contentW());
        addWidget(contentRootPanel);

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL);
        scrollbar.minValue(0);
        scrollbar.onValueChanged(v -> {
            viewport.applyScrollbarValue(v);
            updateWidgetPositions();
        });
        addWidget(scrollbar);

        for (ButtonWidget btn : actionBar.buttons()) {
            addWidget(btn);
        }

        updateLayout();
        updateWidgetPositions();
        applySearchFilter(searchText);
    }

    private void syncContentHeight() {
        if (!applyingSearch && contentRootPanel != null) {
            contentRootPanel.reflowVisibleChildren();
            updateLayout();
            updateWidgetPositions();
        }
    }

    private void updateLayout() {
        int contentHeight = contentRootPanel != null ? (int) contentRootPanel.height() : 0;
        int maxListHeight = Math.max(0,
                actionBar.maxScrollableHeight(viewport.cardH(), CARD_INNER) - SEARCH_HEIGHT - SEARCH_GAP);
        viewport.layoutContent(contentHeight, maxListHeight, scrollbar);
        if (searchInput != null) {
            searchInput.bounds(new ScreenCoordinate(
                    viewport.contentLeft(), viewport.cardY() + CARD_INNER,
                    viewport.contentW(), SEARCH_HEIGHT));
        }
        actionBar.layout(viewport.cardX(), viewport.cardY(), viewport.cardW(), viewport.cardH(), CARD_INNER,
                b -> font.width(b.text().toString()));
    }

    private void updateWidgetPositions() {
        viewport.applyContentBounds(contentRootPanel);
    }

    /**
     * 搜索只改变现有控件的可见性，避免过滤时丢失尚未保存的编辑值。
     */
    private void applySearchFilter(String value) {
        searchText = value == null ? "" : value;
        if (contentRootPanel == null) {
            return;
        }
        ConfigSearchQuery query = ConfigSearchQuery.of(searchText);
        boolean searching = !query.isEmpty();
        if (searching && expandedBeforeSearch.isEmpty()) {
            for (Map.Entry<String, CollapsiblePanelWidget> entry
                    : contentTreeBuilder.categoryPanels().entrySet()) {
                expandedBeforeSearch.put(entry.getKey(), entry.getValue().expanded());
            }
        }

        applyingSearch = true;
        ConfigHolder.CategoryTreeNode root = holder.getCategoryTree().isEmpty()
                ? null : holder.getCategoryTree().get(0);
        if (root != null) {
            applyNodeFilter(root, query, false);
        }
        contentRootPanel.visible(true);
        updateCategoryTitle("", query, false);

        // 清空搜索后恢复玩家原先的展开状态。
        if (!searching) {
            for (Map.Entry<String, CollapsiblePanelWidget> entry
                    : contentTreeBuilder.categoryPanels().entrySet()) {
                Boolean expanded = expandedBeforeSearch.get(entry.getKey());
                entry.getValue().expanded(expanded != null ? expanded : entry.getKey().isEmpty());
            }
            expandedBeforeSearch.clear();
        }
        contentRootPanel.reflowVisibleChildren();
        scrollbar.value(0);
        viewport.applyScrollbarValue(0);
        applyingSearch = false;
        updateLayout();
        updateWidgetPositions();
    }

    private boolean applyNodeFilter(ConfigHolder.CategoryTreeNode node, ConfigSearchQuery query,
                                    boolean ancestorMatches) {
        String categoryPath = node.getCategoryPath();
        String title = contentTreeBuilder.categoryTitles().get(categoryPath);
        boolean categoryMatches = !query.isEmpty()
                && query.matches(categoryPath, title, node.getDisplayName());
        boolean showAll = ancestorMatches || categoryMatches;
        boolean visibleDescendant = false;

        for (ConfigEntryDescriptor descriptor : node.getEntries()) {
            boolean matches = showAll || matchesEntry(descriptor, query);
            ConfigEditorEntryWidget widget = editorState.entryWidgets().get(descriptor.getPath());
            if (widget != null) {
                widget.getWidget().visible(matches);
                updateEntryText(widget, descriptor, query, matches && !query.isEmpty());
            }
            visibleDescendant |= matches;
        }
        for (ConfigHolder.CategoryTreeNode child : node.getChildren()) {
            boolean childVisible = applyNodeFilter(child, query, showAll);
            CollapsiblePanelWidget panel =
                    contentTreeBuilder.categoryPanels().get(child.getCategoryPath());
            if (panel != null) {
                panel.visible(childVisible);
                if (!query.isEmpty() && childVisible) {
                    panel.expanded(true);
                }
            }
            visibleDescendant |= childVisible;
        }

        boolean visible = query.isEmpty() || categoryMatches || visibleDescendant;
        if (!categoryPath.isEmpty()) {
            updateCategoryTitle(categoryPath, query, categoryMatches);
        }
        return visible;
    }

    private boolean matchesEntry(ConfigEntryDescriptor descriptor, ConfigSearchQuery query) {
        String description = entryDescription(descriptor);
        return query.matches(descriptor.getPath(), descriptor.getDisplayName(), description);
    }

    private void updateEntryText(ConfigEditorEntryWidget widget, ConfigEntryDescriptor descriptor,
                                 ConfigSearchQuery query, boolean matched) {
        if (!(widget instanceof ConfigEditorEntryWidgetAdapter)) {
            return;
        }
        BaniraColorConfig theme = getEffectiveTheme();
        ConfigEditorEntryWidgetAdapter adapter = (ConfigEditorEntryWidgetAdapter) widget;
        boolean titleContains = query.indexIn(descriptor.getDisplayName()) >= 0;
        if (adapter.labelWidget() != null) {
            adapter.labelWidget().text(ConfigSearchText.highlight(descriptor.getDisplayName(), query,
                    theme.textPrimary(), theme.searchMatchText(), matched && !titleContains));
        }
        if (adapter.tooltipWidget() != null) {
            adapter.tooltipWidget().text(ConfigSearchText.highlight(entryDescription(descriptor), query,
                    theme.textPrimary(), theme.searchMatchText(), false));
        }
    }

    private String entryDescription(ConfigEntryDescriptor descriptor) {
        if (!ConfigEntryTooltipTexts.hasGuiTooltip(descriptor)) {
            return "";
        }
        return ConfigEntryTooltipTexts.guiTooltipComponent(descriptor, rowFactory.configModId())
                .getString(Translator.getClientLanguage(), true, true);
    }

    private void updateCategoryTitle(String path, ConfigSearchQuery query, boolean matched) {
        CollapsiblePanelWidget panel = contentTreeBuilder.categoryPanels().get(path);
        String title = contentTreeBuilder.categoryTitles().get(path);
        if (panel == null || title == null) {
            return;
        }
        BaniraColorConfig theme = getEffectiveTheme();
        panel.text(ConfigSearchText.highlight(title, query, theme.textPrimary(),
                theme.searchMatchText(), matched && query.indexIn(title) < 0));
    }

    private void saveConfig() {
        editorState.collectModifiedFromWidgets();
        if (editorState.hasInvalidEntryWidgets()) {
            ConfigEditorNotifier.show("config_editor_validation_failed", 3000);
            return;
        }
        editorState.applyModifiedToHolder();
        try {
            holder.save();
            editorState.markClean();
            ConfigEditorNotifier.show("config_editor_save_success", 2000);
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
            ConfigEditorSyncService.applyEncodedValues(holder, toSync);
            editorState.markClean();
        } catch (Exception ex) {
            ConfigEditorNotifier.show("config_editor_sync_failed", 4000,
                    ex.getMessage() != null ? ex.getMessage() : "");
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
            ConfigEditorSyncService.applyEncodedValues(holder, toSync);
            editorState.markClean();
        } catch (Exception ex) {
            ConfigEditorNotifier.show("config_editor_sync_full_failed", 4000,
                    ex.getMessage() != null ? ex.getMessage() : "");
        }
    }

    /**
     * 长按「保存」：向服务端请求当前配置的全量快照并刷新本界面。
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
            ConfigEditorNotifier.show("config_editor_fetch_send_failed", 4000,
                    ex.getMessage() != null ? ex.getMessage() : "");
        }
    }

    /**
     * 收到服务端快照后，用当前 {@link ConfigHolder} 刷新对应界面控件。
     */
    public void refreshUIFromHolderAfterRemoteFetch(String configName) {
        if (!holder.getConfigName().equals(configName)) {
            return;
        }
        editorState.refreshEntriesFromHolder(configName);
    }

    @Override
    protected void onKeyPressed(KeyPressedHandleArgs eventArgs) {
        if (eventArgs.key() != GLFWKey.GLFW_KEY_ESCAPE) {
            return;
        }
        int changedCount = editorState.pendingChangeCount();
        if (changedCount == 0) {
            onClose();
        } else {
            ConfigEditorNotifier.show("config_editor_unsaved_changes", 4500, changedCount);
        }
        eventArgs.consumed(true);
    }

    @Override
    protected void renderWidgets(MatrixStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        actionBar.renderChrome(stack, theme, viewport.cardX(), viewport.cardY(), viewport.cardW(), viewport.cardH(),
                CARD_INNER);

        ConfigEditorViewportRenderer.renderScrolledContent(stack, partialTicks, contentRootPanel, scrollbar,
                viewport.contentLeft(), viewport.listTop(), viewport.contentTotalW(), viewport.listAreaHeight());
        actionBar.renderButtons(stack, partialTicks);
        ConfigEditorViewportRenderer.renderOverlayWidgets(widgets(), stack, partialTicks,
                widget -> widget == contentRootPanel || widget == scrollbar || actionBar.contains(widget));
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
            viewport.scrollBy(delta, scrollbar);
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
