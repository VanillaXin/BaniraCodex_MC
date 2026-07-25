package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.GLFWKey;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.internal.client.*;

import javax.annotation.Nullable;
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
    private final ConfigEditorContentTreeBuilder contentTreeBuilder;
    private final ConfigEditorActionBar actionBar;
    private final ConfigEditorViewportModel viewport;

    private CollapsiblePanelWidget contentRootPanel;
    private ScrollbarWidget scrollbar;

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
        this.viewport = new ConfigEditorViewportModel(CARD_MARGIN, CARD_INNER, SCROLL_WIDTH, SCROLL_GAP);
        previousScreen(args != null ? args.parentScreen() : null);
        BaniraScreen.inheritThemeAndSeason(this, args != null ? args.parentScreen() : null, args != null ? args.theme() : null, args != null ? args.season() : null);
    }

    public static void open(ConfigHolder holder, @Nullable Screen parent) {
        BaniraClientScreenService.openConfigEditor(holder, parent);
    }

    @Override
    protected void initWidgets() {
        viewport.resize(width, height);
        editorState.clearEntries();
        actionBar.rebuildButtons();

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
    }

    private void syncContentHeight() {
        if (contentRootPanel != null) {
            contentRootPanel.refreshLayout();
            updateLayout();
            updateWidgetPositions();
        }
    }

    private void updateLayout() {
        int contentHeight = contentRootPanel != null ? (int) contentRootPanel.height() : 0;
        int maxListHeight = actionBar.maxScrollableHeight(viewport.cardH(), CARD_INNER);
        viewport.layoutContent(contentHeight, maxListHeight, scrollbar);
        actionBar.layout(viewport.cardX(), viewport.cardY(), viewport.cardW(), viewport.cardH(), CARD_INNER,
                b -> font.width(b.text().toString()));
    }

    private void updateWidgetPositions() {
        viewport.applyContentBounds(contentRootPanel);
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
            ConfigEditorSyncService.applyEncodedValues(holder, toSync);
            editorState.markClean();
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
            ConfigEditorSyncService.applyEncodedValues(holder, toSync);
            editorState.markClean();
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
    protected void renderWidgets(PoseStack stack, float partialTicks) {
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
    protected void onRender(PoseStack stack, float partialTicks) {
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
