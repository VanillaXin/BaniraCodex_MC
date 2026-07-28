package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.client.notification.BaniraNotifications;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.event.MouseScrollEvent;
import xin.vanilla.banira.client.gui.search.ConfigSearchQuery;
import xin.vanilla.banira.client.gui.search.ConfigSearchText;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.network.packet.CustomPlayerConfigSyncToServer;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.config.CustomConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 编辑当前玩家的语言与通知偏好。
 */
public class CustomPlayerConfigEditScreen extends BaniraScreen {

    private static final int CARD_MARGIN = 10;
    private static final int CARD_INNER = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_GAP = 2;
    private static final double LABEL_COLUMN_WIDTH_RATIO = 0.32;
    private static final double LABEL_COLUMN_MIN_WIDTH = 64;
    private static final int GAP_LABEL_TO_VALUE = 4;
    private static final double VALUE_AREA_MIN_WIDTH = 56;
    private static final int SCROLL_WIDTH = 6;
    private static final int SCROLL_GAP = 2;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_PADDING = 12;
    private static final int BUTTON_GAP = 8;
    private static final int CARD_GAP = 1;
    private static final int CARD_RADIUS = 8;
    private static final int CARD_ALPHA = 0xFF;
    private static final int SEARCH_HEIGHT = 18;
    private static final int SEARCH_GAP = 4;

    private final Args args;
    private DropdownSelectWidget languageDropdown;
    private DropdownSelectWidget modeDropdown;
    private EntryRowWidget languageRow;
    private EntryRowWidget modeRow;
    private LabelWidget languageLabel;
    private LabelWidget modeLabel;
    private TooltipWidget languageTooltip;
    private TooltipWidget modeTooltip;
    private List<String> languageOptions = new ArrayList<>();
    private String labelBaniraMode;
    private String labelVanillaMode;

    private CollapsiblePanelWidget contentRootPanel;
    private InputWidget searchInput;
    private ScrollbarWidget scrollbar;
    private double scrollOffset;
    private int contentHeight;
    private int cardX;
    private int cardY;
    private int cardW;
    private int cardH;
    private int listTop;
    private int listAreaHeight;
    private int maxListHeight;
    private int contentLeft;
    private int contentW;
    private int btnY;
    private int contentTotalW;
    private final List<ButtonWidget> bottomButtons = new ArrayList<>();
    private String searchText = "";

    public CustomPlayerConfigEditScreen(@Nullable Args args) {
        super(BaniraComponent.get().transClientAuto("custom_player_config_title").toVanilla());
        this.args = args != null ? args : new Args();
        previousScreen(this.args.parentScreen());
        BaniraScreen.inheritThemeAndSeason(this, this.args.parentScreen(), this.args.theme(), this.args.season());
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Args {
        @Nullable
        private Screen parentScreen;
        @Nullable
        private BaniraColorConfig theme;
        @Nullable
        private EnumSeason season;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void initWidgets() {
        cardX = CARD_MARGIN;
        cardY = CARD_MARGIN;
        cardW = width - CARD_MARGIN * 2;
        cardH = height - CARD_MARGIN * 2;
        contentLeft = cardX + CARD_INNER;
        contentW = cardW - CARD_INNER * 2 - SCROLL_WIDTH - SCROLL_GAP;
        contentTotalW = contentW + SCROLL_GAP + SCROLL_WIDTH;
        listTop = cardY + CARD_INNER + SEARCH_HEIGHT + SEARCH_GAP;
        bottomButtons.clear();

        initializeOptions();
        searchInput = new InputWidget(this);
        searchInput.id("custom_player_config_search");
        searchInput.text(BaniraComponent.get().transClientAuto("config_search_hint"));
        searchInput.value(searchText);
        searchInput.onTextChanged(this::applySearchFilter);
        addWidget(searchInput);

        contentRootPanel = buildContentPanel();
        contentHeight = (int) contentRootPanel.height();
        addWidget(contentRootPanel);

        scrollbar = new ScrollbarWidget(this);
        scrollbar.id("custom_player_config_scroll");
        scrollbar.orientation(EnumOrientation.VERTICAL);
        scrollbar.minValue(0);
        scrollbar.onValueChanged(v -> {
            scrollOffset = v;
            updateWidgetPositions();
        });
        addWidget(scrollbar);

        ButtonWidget saveBtn = new ButtonWidget(this);
        saveBtn.id("custom_player_config_save");
        saveBtn.text(BaniraComponent.get().transClientAuto("config_editor_save").toString());
        saveBtn.onClick(b -> syncToServer());
        bottomButtons.add(saveBtn);

        ButtonWidget closeBtn = new ButtonWidget(this);
        closeBtn.id("custom_player_config_close");
        closeBtn.text(BaniraComponent.get().transClientAuto("config_editor_close").toString());
        closeBtn.onClick(b -> onClose());
        bottomButtons.add(closeBtn);

        for (ButtonWidget button : bottomButtons) {
            addWidget(button);
        }

        updateLayout();
        updateWidgetPositions();
        applySearchFilter(searchText);
    }

    private void initializeOptions() {
        labelBaniraMode = BaniraComponent.get().transClientAuto("custom_player_config_mode_banira").toString();
        labelVanillaMode = BaniraComponent.get().transClientAuto("custom_player_config_mode_vanilla").toString();

        languageOptions = new ArrayList<>();
        languageOptions.add("client");
        languageOptions.add("server");
        Translator translator = (Translator) Translator.of(Banira.MOD_ID);
        languageOptions.addAll(translator.getI18nFiles());
    }

    private CollapsiblePanelWidget buildContentPanel() {
        CollapsiblePanelWidget root = CollapsiblePanelWidget.createAutoHeight(this, 0, 0, contentW);
        root.text(Text.from(BaniraComponent.get().transClientAuto("custom_player_config_title")));
        root.expanded(true);
        root.contentGap(ROW_GAP);
        root.headerHeight(ROW_HEIGHT);
        root.onExpandChanged(panel -> syncContentHeight());

        double rowWidth = root.getContentWidth();
        languageRow = createLanguageRow(rowWidth);
        modeRow = createNotificationModeRow(rowWidth);
        root.addChildAuto(languageRow, ROW_HEIGHT);
        root.addChildAuto(modeRow, ROW_HEIGHT);
        root.refreshLayout();
        return root;
    }

    private EntryRowWidget createLanguageRow(double rowWidth) {
        EntryRowWidget row = createRow(rowWidth);
        languageLabel = createLabel("custom_player_config_language_label",
                BaniraComponent.get().transClientAuto("custom_player_config_language"), rowWidth);
        row.addChild(languageLabel);
        languageTooltip = createDescriptionTooltip("custom_player_config_language_description", rowWidth);
        row.addChild(languageTooltip);

        languageDropdown = new DropdownSelectWidget(this);
        languageDropdown.id("custom_player_config_language");
        languageDropdown.bounds(new ScreenCoordinate(valueStartX(rowWidth), 0,
                valueWidgetWidth(rowWidth), ROW_HEIGHT));
        languageDropdown.options(languageOptions);

        String uuid = currentPlayerUuid();
        String selected = uuid.isEmpty() ? "client" : CustomConfig.getPlayerLanguageClient(uuid);
        if (!languageOptions.contains(selected)) {
            selected = languageOptions.get(0);
        }
        languageDropdown.selectedValues(Collections.singletonList(selected));
        row.addChild(languageDropdown);
        return row;
    }

    private EntryRowWidget createNotificationModeRow(double rowWidth) {
        EntryRowWidget row = createRow(rowWidth);
        modeLabel = createLabel("custom_player_config_mode_label",
                BaniraComponent.get().transClientAuto("custom_player_config_notification_mode"), rowWidth);
        row.addChild(modeLabel);
        modeTooltip = createDescriptionTooltip("custom_player_config_notification_mode_description", rowWidth);
        row.addChild(modeTooltip);

        List<String> modeLabels = new ArrayList<>();
        modeLabels.add(labelBaniraMode);
        modeLabels.add(labelVanillaMode);

        modeDropdown = new DropdownSelectWidget(this);
        modeDropdown.id("custom_player_config_mode");
        modeDropdown.bounds(new ScreenCoordinate(valueStartX(rowWidth), 0,
                valueWidgetWidth(rowWidth), ROW_HEIGHT));
        modeDropdown.options(modeLabels);

        String uuid = currentPlayerUuid();
        String currentMode = uuid.isEmpty()
                ? CustomConfig.notificationReceiveModeNotification
                : CustomConfig.getPlayerNotificationReceiveModeClient(uuid);
        String selected = CustomConfig.notificationReceiveModeVanillaMessage.equals(currentMode)
                ? labelVanillaMode : labelBaniraMode;
        modeDropdown.selectedValues(Collections.singletonList(selected));
        row.addChild(modeDropdown);
        return row;
    }

    private EntryRowWidget createRow(double rowWidth) {
        EntryRowWidget row = new EntryRowWidget(this);
        row.bounds(new ScreenCoordinate(0, 0, rowWidth, ROW_HEIGHT));
        return row;
    }

    private LabelWidget createLabel(String id, xin.vanilla.banira.common.data.Component text, double rowWidth) {
        LabelWidget label = new LabelWidget(this);
        label.id(id);
        label.bounds(new ScreenCoordinate(0, 0, labelTextWidth(rowWidth), ROW_HEIGHT));
        label.text(Text.from(text));
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);
        return label;
    }

    private TooltipWidget createDescriptionTooltip(String translationKey, double rowWidth) {
        TooltipWidget tooltip = new TooltipWidget(this,
                new ScreenCoordinate(0, 0, labelTextWidth(rowWidth), ROW_HEIGHT));
        tooltip.id(translationKey + "_tooltip");
        tooltip.text(BaniraComponent.get().transClientAuto(translationKey));
        tooltip.popupAtScreenCoords(true);
        return tooltip;
    }

    private double labelColumnEndX(double rowWidth) {
        if (rowWidth <= 1) {
            return 1;
        }
        double maxEnd = rowWidth - VALUE_AREA_MIN_WIDTH;
        if (maxEnd < 1) {
            return Math.max(1, rowWidth * 0.2);
        }
        double fromRatio = rowWidth * LABEL_COLUMN_WIDTH_RATIO;
        return Math.min(Math.max(LABEL_COLUMN_MIN_WIDTH, Math.min(fromRatio, maxEnd)), maxEnd);
    }

    private double labelTextWidth(double rowWidth) {
        return Math.max(1, labelColumnEndX(rowWidth) - GAP_LABEL_TO_VALUE);
    }

    private double valueStartX(double rowWidth) {
        return labelColumnEndX(rowWidth);
    }

    private double valueWidgetWidth(double rowWidth) {
        return Math.max(1, rowWidth - labelColumnEndX(rowWidth));
    }

    private void syncToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            notifySyncFailure("custom_player_config_sync_not_connected", "");
            return;
        }
        if (minecraft.player == null) {
            return;
        }

        String uuid = PlayerUtils.getPlayerUUIDString(minecraft.player);
        List<String> languageSelection = languageDropdown.getSelectedValues();
        String language = languageSelection.isEmpty() ? "client" : languageSelection.get(0);
        List<String> modeSelection = modeDropdown.getSelectedValues();
        String modeLabel = modeSelection.isEmpty() ? labelBaniraMode : modeSelection.get(0);
        String mode = labelVanillaMode.equals(modeLabel)
                ? CustomConfig.notificationReceiveModeVanillaMessage
                : CustomConfig.notificationReceiveModeNotification;

        try {
            PacketUtils.sendPacketToServer(new CustomPlayerConfigSyncToServer(language, mode));
            CustomConfig.setPlayerLanguageClient(uuid, language);
            CustomConfig.setPlayerNotificationReceiveModeClient(uuid, mode);
            onClose();
        } catch (Exception exception) {
            notifySyncFailure("custom_player_config_sync_failed",
                    exception.getMessage() != null ? exception.getMessage() : "");
        }
    }

    private void notifySyncFailure(String key, String detail) {
        Notification notification = detail.isEmpty()
                ? Notification.ofComponent(BaniraComponent.get().transClientAuto(key))
                : Notification.ofComponent(BaniraComponent.get().transClientAuto(key, detail));
        notification.position(EnumPosition.TOP_RIGHT).durationTime(4000);
        BaniraNotifications.show(notification);
    }

    private String currentPlayerUuid() {
        return Minecraft.getInstance().player != null
                ? PlayerUtils.getPlayerUUIDString(Minecraft.getInstance().player)
                : "";
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
        maxListHeight = Math.max(0, cardH - CARD_INNER * 2 - SEARCH_HEIGHT - SEARCH_GAP
                - BUTTON_HEIGHT - CARD_GAP);
        if (searchInput != null) {
            searchInput.bounds(new ScreenCoordinate(contentLeft, cardY + CARD_INNER, contentW, SEARCH_HEIGHT));
        }
        int buttonAreaHeight = BUTTON_HEIGHT + CARD_INNER;
        int buttonAreaTop = cardY + cardH - buttonAreaHeight;
        btnY = buttonAreaTop + (buttonAreaHeight - BUTTON_HEIGHT) / 2;

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
            scrollbar.bounds(new ScreenCoordinate(contentLeft + contentW + SCROLL_GAP,
                    listTop, SCROLL_WIDTH, listAreaHeight));
            scrollbar.maxValue(Math.max(0, contentHeight - listAreaHeight));
            scrollbar.value(Math.min(scrollOffset, scrollbar.maxValue()));
            scrollOffset = scrollbar.value();
            scrollbar.visibleSize(listAreaHeight);
            scrollbar.scrollingCoordinates(new ArrayList<>());
            scrollbar.addScrollHoverArea(new ScreenCoordinate(contentLeft, listTop,
                    contentTotalW, listAreaHeight));
        }

        int[] buttonWidths = new int[bottomButtons.size()];
        for (int i = 0; i < bottomButtons.size(); i++) {
            buttonWidths[i] = font.width(bottomButtons.get(i).text().toString()) + BUTTON_PADDING * 2;
        }

        int contentTotal = cardW - CARD_INNER * 2 - CARD_GAP;
        int zoneWidth = contentTotal / 2;
        int leftRectWidth = CARD_INNER + zoneWidth;
        int rightRectWidth = cardW - leftRectWidth - CARD_GAP;
        int rightRectX = cardX + leftRectWidth + CARD_GAP;
        int leftWidth = Math.min(buttonWidths[0], Math.max(20, zoneWidth));
        int rightWidth = Math.min(buttonWidths[1], Math.max(20, zoneWidth));
        bottomButtons.get(0).bounds(new ScreenCoordinate(
                cardX + (leftRectWidth - leftWidth) / 2, btnY, leftWidth, BUTTON_HEIGHT));
        bottomButtons.get(1).bounds(new ScreenCoordinate(
                rightRectX + (rightRectWidth - rightWidth) / 2, btnY, rightWidth, BUTTON_HEIGHT));
    }

    private void updateWidgetPositions() {
        if (contentRootPanel != null) {
            contentRootPanel.bounds(new ScreenCoordinate(contentLeft,
                    listTop - (int) scrollOffset, contentW, contentHeight));
        }
    }

    private void applySearchFilter(String value) {
        searchText = value == null ? "" : value;
        if (contentRootPanel == null || languageRow == null || modeRow == null) {
            return;
        }
        ConfigSearchQuery query = ConfigSearchQuery.of(searchText);
        String rootTitle = BaniraComponent.get().transClientAuto("custom_player_config_title").toString();
        boolean rootMatches = !query.isEmpty() && query.matches(rootTitle, "player");
        String languageTitle = BaniraComponent.get().transClientAuto("custom_player_config_language").toString();
        String languageDescription = BaniraComponent.get()
                .transClientAuto("custom_player_config_language_description").toString();
        String modeTitle = BaniraComponent.get().transClientAuto("custom_player_config_notification_mode").toString();
        String modeDescription = BaniraComponent.get()
                .transClientAuto("custom_player_config_notification_mode_description").toString();

        boolean languageSelfMatches = !query.isEmpty() && query.matches(
                "language", "player.language", languageTitle, languageDescription);
        boolean modeSelfMatches = !query.isEmpty() && query.matches(
                "notificationReceiveMode", "player.notificationReceiveMode", modeTitle, modeDescription);
        languageRow.visible(query.isEmpty() || rootMatches || languageSelfMatches);
        modeRow.visible(query.isEmpty() || rootMatches || modeSelfMatches);

        BaniraColorConfig theme = getEffectiveTheme();
        contentRootPanel.text(ConfigSearchText.highlight(rootTitle, query, theme.textPrimary(),
                theme.searchMatchText()));
        languageLabel.text(ConfigSearchText.highlight(languageTitle, query, theme.textPrimary(),
                theme.searchMatchText()));
        modeLabel.text(ConfigSearchText.highlight(modeTitle, query, theme.textPrimary(),
                theme.searchMatchText()));
        languageTooltip.text(ConfigSearchText.highlight(languageDescription, query, theme.textPrimary(),
                theme.searchMatchText()));
        modeTooltip.text(ConfigSearchText.highlight(modeDescription, query, theme.textPrimary(),
                theme.searchMatchText()));

        contentRootPanel.reflowVisibleChildren();
        contentHeight = (int) contentRootPanel.height();
        scrollOffset = 0;
        updateLayout();
        updateWidgetPositions();
    }

    @Override
    protected void renderWidgets(MatrixStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        int cardBackground = ColorUtils.applyAlphaToArgb(theme.bgSurface(), CARD_ALPHA);
        int buttonAreaHeight = BUTTON_HEIGHT + CARD_INNER;
        int buttonAreaTop = cardY + cardH - buttonAreaHeight;
        int contentDrawHeight = buttonAreaTop - cardY - CARD_GAP;

        AbstractGuiUtils.drawRoundedRect(stack, cardX, cardY, cardW, contentDrawHeight,
                CARD_RADIUS, CARD_RADIUS, 0, 0, cardBackground);

        int contentTotal = cardW - CARD_INNER * 2 - CARD_GAP;
        int zoneWidth = contentTotal / 2;
        int leftRectWidth = CARD_INNER + zoneWidth;
        int rightRectWidth = cardW - leftRectWidth - CARD_GAP;
        AbstractGuiUtils.drawRoundedRect(stack, cardX, buttonAreaTop, leftRectWidth, buttonAreaHeight,
                0, 0, CARD_RADIUS, 0, cardBackground);
        AbstractGuiUtils.drawRoundedRect(stack, cardX + leftRectWidth + CARD_GAP,
                buttonAreaTop, rightRectWidth, buttonAreaHeight,
                0, 0, 0, CARD_RADIUS, cardBackground);

        AbstractGuiUtils.enableScissor(contentLeft, listTop, contentTotalW, Math.max(1, listAreaHeight));
        if (contentRootPanel != null && contentRootPanel.visible()) {
            if (contentRootPanel.enabled() && contentRootPanel.needsUpdate()) {
                contentRootPanel.update();
            }
            contentRootPanel.render(stack, partialTicks);
        }
        if (scrollbar != null && scrollbar.visible()) {
            if (scrollbar.enabled() && scrollbar.needsUpdate()) {
                scrollbar.update();
            }
            scrollbar.render(stack, partialTicks);
        }
        AbstractGuiUtils.disableScissor();

        for (ButtonWidget button : bottomButtons) {
            if (button.visible()) {
                if (button.enabled() && button.needsUpdate()) {
                    button.update();
                }
                button.render(stack, partialTicks);
            }
        }
        for (IWidget widget : widgets()) {
            if (widget == contentRootPanel || widget == scrollbar || bottomButtons.contains(widget)) {
                continue;
            }
            if (widget.parent() != null || !widget.visible()) {
                continue;
            }
            if (widget.enabled() && widget.needsUpdate()) {
                widget.update();
            }
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
            double newValue = Math.max(scrollbar.minValue(),
                    Math.min(scrollbar.maxValue(), scrollbar.value() - delta * 20));
            scrollbar.value(newValue);
            scrollOffset = newValue;
            updateWidgetPositions();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (args.parentScreen() != null) {
            Minecraft.getInstance().setScreen(args.parentScreen());
        } else {
            super.onClose();
        }
    }

    private static final class EntryRowWidget extends BaseWidget {
        private EntryRowWidget(BaniraScreen screen) {
            super(screen);
        }

        @Override
        public double effectiveHeight() {
            double maxBottom = 0;
            for (IWidget child : children()) {
                if (child == null || !child.visible()) {
                    continue;
                }
                ScreenCoordinate bounds = child.bounds();
                if (bounds != null) {
                    maxBottom = Math.max(maxBottom, bounds.y() + child.effectiveHeight());
                }
            }
            return maxBottom > 0 ? maxBottom : (bounds() != null ? bounds().height() : 0);
        }

        @Override
        protected boolean onMouseClick(MouseEvent event) {
            return true;
        }

        @Override
        public void render(MatrixStack stack, float partialTicks) {
            if (visible()) {
                renderChildren(stack, partialTicks);
            }
        }
    }
}
