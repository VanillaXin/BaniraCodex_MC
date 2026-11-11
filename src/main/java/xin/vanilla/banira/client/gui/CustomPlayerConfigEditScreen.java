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
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.network.packet.CustomPlayerConfigSyncToServer;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.ui.UiViewState;
import xin.vanilla.banira.internal.client.ui.page.PlayerConfigUiSession;
import xin.vanilla.banira.internal.config.CustomConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 编辑当前玩家的语言与通知偏好。
 */
public class CustomPlayerConfigEditScreen extends PlayerConfigScreen {
    private static final double LABEL_COLUMN_WIDTH_RATIO = 0.32;
    private static final double LABEL_COLUMN_MIN_WIDTH = 64;
    private static final int GAP_LABEL_TO_VALUE = 4;
    private static final double VALUE_AREA_MIN_WIDTH = 56;

    private DropdownSelectWidget languageDropdown;
    private DropdownSelectWidget modeDropdown;
    private List<String> languageOptions = new ArrayList<>();
    private String labelBaniraMode;
    private String labelVanillaMode;
    private PlayerConfigUiSession uiSession;

    @Override
    public PlayerConfigUiSession playerUiSession() {
        if (uiSession == null) {
            initializeOptions();
            String uuid = currentPlayerUuid();
            String language = uuid.isEmpty() ? "client" : CustomConfig.getPlayerLanguageClient(uuid);
            if (!languageOptions.contains(language)) language = languageOptions.get(0);
            String mode = uuid.isEmpty() ? CustomConfig.notificationReceiveModeNotification : CustomConfig.getPlayerNotificationReceiveModeClient(uuid);
            InputFormScreen.Args form = new InputFormScreen.Args().setParentScreen(this)
                    .setHeaderTitle(Text.from(BaniraComponent.get().transClientAuto("custom_player_config_title")))
                    .addWidget(new InputFormScreen.Widget().name("language").type(InputFormScreen.WidgetType.DROPDOWN)
                            .title(Text.from(BaniraComponent.get().transClientAuto("custom_player_config_language")))
                            .tooltip(Text.from(BaniraComponent.get().transClientAuto("custom_player_config_language_description")))
                            .dropdownOptions(languageOptions).defaultValue(language))
                    .addWidget(new InputFormScreen.Widget().name("notificationReceiveMode").type(InputFormScreen.WidgetType.DROPDOWN)
                            .title(Text.from(BaniraComponent.get().transClientAuto("custom_player_config_notification_mode")))
                            .tooltip(Text.from(BaniraComponent.get().transClientAuto("custom_player_config_notification_mode_description")))
                            .dropdownOptionEntries(Arrays.asList(
                                    new DropdownOption(CustomConfig.notificationReceiveModeNotification, labelBaniraMode),
                                    new DropdownOption(CustomConfig.notificationReceiveModeVanillaMessage, labelVanillaMode)))
                            .defaultValue(CustomConfig.notificationReceiveModeVanillaMessage.equals(mode)
                                    ? CustomConfig.notificationReceiveModeVanillaMessage : CustomConfig.notificationReceiveModeNotification))
                    .setCallback(this::persistPlayerConfig);
            uiSession = new PlayerConfigUiSession(form, () -> CustomConfig.playerPreferencesClient(uuid));
        }
        return uiSession;
    }

    @Override
    public void tick() {
        super.tick();
        if (uiSession == null || languageDropdown == null || modeDropdown == null) return;
        captureUiState(null);
        refreshPlayerControls();
        playerSaveEnabled(uiSession.fields().canSubmit());
    }

    private void refreshPlayerControls() {
        if (!uiSession.refresh()) return;
        languageDropdown.selectedValues(Collections.singletonList(uiSession.fields().draft("language")));
        modeDropdown.selectedValues(Collections.singletonList(CustomConfig.notificationReceiveModeVanillaMessage
                .equals(uiSession.fields().draft("notificationReceiveMode")) ? labelVanillaMode : labelBaniraMode));
    }

    @Override
    protected void afterPlayerConfigBuilt() {
        playerSaveEnabled(playerUiSession().fields().canSubmit());
    }

    @Override
    public void captureUiState(UiViewState state) {
        if (languageDropdown != null && modeDropdown != null) {
            List<String> languages = languageDropdown.getSelectedValues();
            List<String> modes = modeDropdown.getSelectedValues();
            playerUiSession().fields().acceptNative(0, languages.isEmpty() ? "client" : languages.get(0));
            playerUiSession().fields().acceptNative(1, !modes.isEmpty() && labelVanillaMode.equals(modes.get(0))
                    ? CustomConfig.notificationReceiveModeVanillaMessage : CustomConfig.notificationReceiveModeNotification);
        }
        super.captureUiState(state);
    }

    public CustomPlayerConfigEditScreen(@Nullable Args args) {
        this(args != null ? args : new Args(), true);
    }

    private CustomPlayerConfigEditScreen(Args args, boolean ignored) {
        super(Banira.MOD_ID, BaniraComponent.get().transClientAuto("custom_player_config_title"),
                args.parentScreen(), args.theme(), args.season());
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
    protected void buildPlayerConfig(CollapsiblePanelWidget root) {
        initializeOptions();
        double rowWidth = root.getContentWidth();

        Component languageTitle = BaniraComponent.get().transClientAuto("custom_player_config_language");
        Component languageDescription = BaniraComponent.get()
                .transClientAuto("custom_player_config_language_description");
        EntryRowWidget languageRow = createRow(rowWidth);
        LabelWidget languageLabel = createLabel("custom_player_config_language_label", languageTitle, rowWidth);
        TooltipWidget languageTooltip = createDescriptionTooltip(
                "custom_player_config_language_description", languageDescription, rowWidth);
        languageRow.addChild(languageLabel);
        languageRow.addChild(languageTooltip);

        languageDropdown = new DropdownSelectWidget(this);
        languageDropdown.id("custom_player_config_language");
        languageDropdown.bounds(new ScreenCoordinate(valueStartX(rowWidth), 0,
                valueWidgetWidth(rowWidth), ROW_HEIGHT));
        languageDropdown.options(languageOptions);
        String selectedLanguage = playerUiSession().fields().draft("language");
        if (!languageOptions.contains(selectedLanguage)) selectedLanguage = languageOptions.get(0);
        languageDropdown.selectedValues(Collections.singletonList(selectedLanguage));
        languageRow.addChild(languageDropdown);
        addPlayerRow(root, languageRow, ROW_HEIGHT, languageLabel, languageTooltip,
                languageTitle, languageDescription, "language", "player.language");

        Component modeTitle = BaniraComponent.get().transClientAuto("custom_player_config_notification_mode");
        Component modeDescription = BaniraComponent.get()
                .transClientAuto("custom_player_config_notification_mode_description");
        EntryRowWidget modeRow = createRow(rowWidth);
        LabelWidget modeLabel = createLabel("custom_player_config_mode_label", modeTitle, rowWidth);
        TooltipWidget modeTooltip = createDescriptionTooltip(
                "custom_player_config_notification_mode_description", modeDescription, rowWidth);
        modeRow.addChild(modeLabel);
        modeRow.addChild(modeTooltip);

        modeDropdown = new DropdownSelectWidget(this);
        modeDropdown.id("custom_player_config_mode");
        modeDropdown.bounds(new ScreenCoordinate(valueStartX(rowWidth), 0,
                valueWidgetWidth(rowWidth), ROW_HEIGHT));
        modeDropdown.options(Arrays.asList(labelBaniraMode, labelVanillaMode));
        String currentMode = playerUiSession().fields().draft("notificationReceiveMode");
        modeDropdown.selectedValues(Collections.singletonList(
                CustomConfig.notificationReceiveModeVanillaMessage.equals(currentMode)
                        ? labelVanillaMode : labelBaniraMode));
        modeRow.addChild(modeDropdown);
        addPlayerRow(root, modeRow, ROW_HEIGHT, modeLabel, modeTooltip,
                modeTitle, modeDescription, "notificationReceiveMode", "player.notificationReceiveMode");
    }

    private void initializeOptions() {
        labelBaniraMode = BaniraComponent.get().transClientAuto("custom_player_config_mode_banira").toString();
        labelVanillaMode = BaniraComponent.get().transClientAuto("custom_player_config_mode_vanilla").toString();
        languageOptions = new ArrayList<>();
        languageOptions.add("client");
        languageOptions.add("server");
        languageOptions.addAll(((Translator) Translator.of(Banira.MOD_ID)).getI18nFiles());
    }

    private EntryRowWidget createRow(double rowWidth) {
        EntryRowWidget row = new EntryRowWidget(this);
        row.bounds(new ScreenCoordinate(0, 0, rowWidth, ROW_HEIGHT));
        return row;
    }

    private LabelWidget createLabel(String id, Component text, double rowWidth) {
        LabelWidget label = new LabelWidget(this);
        label.id(id);
        label.bounds(new ScreenCoordinate(0, 0, labelTextWidth(rowWidth), ROW_HEIGHT));
        label.text(Text.from(text));
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);
        return label;
    }

    private TooltipWidget createDescriptionTooltip(String id, Component text, double rowWidth) {
        TooltipWidget tooltip = new TooltipWidget(this,
                new ScreenCoordinate(0, 0, labelTextWidth(rowWidth), ROW_HEIGHT));
        tooltip.id(id + "_tooltip");
        tooltip.text(text);
        tooltip.popupAtScreenCoords(true);
        return tooltip;
    }

    private double labelColumnEndX(double rowWidth) {
        double maxEnd = rowWidth - VALUE_AREA_MIN_WIDTH;
        if (maxEnd < 1) return Math.max(1, rowWidth * 0.2);
        return Math.min(Math.max(LABEL_COLUMN_MIN_WIDTH,
                Math.min(rowWidth * LABEL_COLUMN_WIDTH_RATIO, maxEnd)), maxEnd);
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

    @Override
    protected void savePlayerConfig() {
        captureUiState(null);
        refreshPlayerControls();
        if (playerUiSession().fields().submit()) onClose();
    }

    private void persistPlayerConfig(InputFormScreen.Results result) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            notifySyncFailure("custom_player_config_sync_not_connected", "");
            result.runningResult(BaniraComponent.get().transClientAuto("custom_player_config_sync_not_connected").getString(Translator.getClientLanguage()));
            return;
        }
        if (minecraft.player == null) {
            result.runningResult(BaniraComponent.get().transClientAuto("custom_player_config_sync_not_connected").getString(Translator.getClientLanguage()));
            return;
        }

        String language = result.value("language");
        String mode = result.value("notificationReceiveMode");
        try {
            PacketUtils.sendPacketToServer(new CustomPlayerConfigSyncToServer(language, mode));
        } catch (Exception exception) {
            result.runningResult(exception);
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
                ? PlayerUtils.getPlayerUUIDString(Minecraft.getInstance().player) : "";
    }

    private static final class EntryRowWidget extends BaseWidget {
        private EntryRowWidget(BaniraScreen screen) {
            super(screen);
        }

        @Override
        public double effectiveHeight() {
            double maxBottom = 0;
            for (IWidget child : children()) {
                if (child == null || !child.visible() || child.bounds() == null) continue;
                maxBottom = Math.max(maxBottom, child.bounds().y() + child.effectiveHeight());
            }
            return maxBottom > 0 ? maxBottom : (bounds() != null ? bounds().height() : 0);
        }

        @Override
        protected boolean onMouseClick(MouseEvent event) {
            return true;
        }

        @Override
        public void render(MatrixStack graphics, float partialTicks) {
            if (visible()) renderChildren(graphics, partialTicks);
        }
    }
}
