package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.client.notification.BaniraNotifications;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.DropdownSelectWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.network.packet.CustomPlayerConfigSyncToServer;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.Translator;
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
        String selectedLanguage = currentPlayerUuid().isEmpty()
                ? "client" : CustomConfig.getPlayerLanguageClient(currentPlayerUuid());
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
        String currentMode = currentPlayerUuid().isEmpty()
                ? CustomConfig.notificationReceiveModeNotification
                : CustomConfig.getPlayerNotificationReceiveModeClient(currentPlayerUuid());
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            notifySyncFailure("custom_player_config_sync_not_connected", "");
            return;
        }
        if (minecraft.player == null) return;

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
        public void render(PoseStack stack, float partialTicks) {
            if (visible()) renderChildren(stack, partialTicks);
        }
    }
}
