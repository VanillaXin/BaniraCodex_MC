package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.DropdownSelectWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.network.packet.CustomPlayerConfigSyncToServer;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.Translator;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.config.CustomConfig;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static xin.vanilla.banira.client.data.BaniraColorToken.BG_SURFACE;

/**
 * 编辑 CustomConfig 中当前玩家的配置
 */
public class CustomPlayerConfigEditScreen extends BaniraScreen {

    private static final int CARD_MARGIN = 16;
    /**
     * 表单区与面板（圆角矩形）左右留白
     */
    private static final int PANEL_INNER_PAD_X = 28;
    private static final int MAX_FORM_W = 400;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 8;
    private static final int LABEL_H = 14;
    private static final int CARD_RADIUS = 8;
    private static final float CLOSE_BTN_SIZE = 10f;
    private static final float CLOSE_BTN_PAD = 6f;

    private final Args args;
    private DropdownSelectWidget languageDropdown;
    private DropdownSelectWidget modeDropdown;
    private List<String> languageOptions = new ArrayList<>();
    private String labelBaniraMode;
    private String labelVanillaMode;

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
    protected void initWidgets() {
        int innerW = width - CARD_MARGIN * 2;
        int availableFormW = Math.max(120, innerW - PANEL_INNER_PAD_X * 2);
        int panelW = Math.min(MAX_FORM_W, availableFormW);
        int cx = CARD_MARGIN + (innerW - panelW) / 2;

        int innerH = height - CARD_MARGIN * 2;
        int contentH = (LABEL_H + 4) + ROW_H + (ROW_GAP + 4)
                + (LABEL_H + 4) + ROW_H + (ROW_GAP + 12)
                + ROW_H;
        int y = CARD_MARGIN + Math.max(0, (innerH - contentH) / 2);

        labelBaniraMode = BaniraComponent.get().transClientAuto("custom_player_config_mode_banira").toString();
        labelVanillaMode = BaniraComponent.get().transClientAuto("custom_player_config_mode_vanilla").toString();

        languageOptions = new ArrayList<>();
        languageOptions.add("client");
        languageOptions.add("server");
        Translator tr = (Translator) Translator.of(Banira.MOD_ID);
        languageOptions.addAll(tr.getI18nFiles());

        var player = BaniraClientRuntime.localPlayer();
        String uuid = player != null ? PlayerUtils.getPlayerUUIDString(player) : "";

        LabelWidget langLabel = new LabelWidget(this);
        langLabel.id("lbl_language");
        langLabel.bounds(new ScreenCoordinate(cx, y, panelW, LABEL_H));
        langLabel.text(BaniraComponent.get().transClientAuto("custom_player_config_language"));
        langLabel.textWrap(false);
        langLabel.textVerticalAlign(EnumAlignment.CENTER);
        addWidget(langLabel);
        y += LABEL_H + 4;

        languageDropdown = new DropdownSelectWidget(this);
        languageDropdown.id("dropdown_language");
        languageDropdown.bounds(new ScreenCoordinate(cx, y, panelW, ROW_H));
        languageDropdown.options(languageOptions);
        String currentLang = uuid.isEmpty() ? "client" : CustomConfig.getPlayerLanguageClient(uuid);
        if (!languageOptions.contains(currentLang)) {
            currentLang = languageOptions.get(0);
        }
        languageDropdown.selectedValues(java.util.Collections.singletonList(currentLang));
        addWidget(languageDropdown);
        y += ROW_H + ROW_GAP + 4;

        LabelWidget modeLabel = new LabelWidget(this);
        modeLabel.id("lbl_mode");
        modeLabel.bounds(new ScreenCoordinate(cx, y, panelW, LABEL_H));
        modeLabel.text(BaniraComponent.get().transClientAuto("custom_player_config_notification_mode"));
        modeLabel.textWrap(false);
        modeLabel.textVerticalAlign(EnumAlignment.CENTER);
        addWidget(modeLabel);
        y += LABEL_H + 4;

        List<String> modeLabels = new ArrayList<>();
        modeLabels.add(labelBaniraMode);
        modeLabels.add(labelVanillaMode);
        modeDropdown = new DropdownSelectWidget(this);
        modeDropdown.id("dropdown_mode");
        modeDropdown.bounds(new ScreenCoordinate(cx, y, panelW, ROW_H));
        modeDropdown.options(modeLabels);
        String currentMode = uuid.isEmpty()
                ? CustomConfig.notificationReceiveModeNotification
                : CustomConfig.getPlayerNotificationReceiveModeClient(uuid);
        String selectedLabel = CustomConfig.notificationReceiveModeVanillaMessage.equals(currentMode) ? labelVanillaMode : labelBaniraMode;
        modeDropdown.selectedValues(java.util.Collections.singletonList(selectedLabel));
        addWidget(modeDropdown);
        y += ROW_H + ROW_GAP + 12;

        ButtonWidget syncBtn = new ButtonWidget(this);
        syncBtn.id("sync");
        syncBtn.bounds(new ScreenCoordinate(cx, y, panelW, ROW_H));
        syncBtn.text(BaniraComponent.get().transClientAuto("custom_player_config_sync").toString());
        syncBtn.onClick(b -> syncToServer());
        addWidget(syncBtn);

        ButtonWidget closeBtn = new ButtonWidget(this);
        closeBtn.id("close");
        closeBtn.bounds(new ScreenCoordinate(width - CARD_MARGIN - CLOSE_BTN_PAD - CLOSE_BTN_SIZE, CARD_MARGIN + CLOSE_BTN_PAD, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE));
        closeBtn.presetStyleClose();
        closeBtn.radius(CLOSE_BTN_SIZE / 3f);
        closeBtn.padding(1);
        closeBtn.onClick(b -> onClose());
        addWidget(closeBtn);
    }

    private void syncToServer() {
        if (!BaniraClientRuntime.hasConnection()) {
            Notification n = Notification.ofComponent(BaniraComponent.get().transClientAuto("custom_player_config_sync_not_connected"));
            n.position(EnumPosition.TOP_RIGHT).durationTime(3500);
            NotificationManager.get().addNotification(n);
            return;
        }
        var player = BaniraClientRuntime.localPlayer();
        if (player == null) {
            return;
        }
        String uuid = PlayerUtils.getPlayerUUIDString(player);
        List<String> langSel = languageDropdown.getSelectedValues();
        String lang = langSel.isEmpty() ? "client" : langSel.get(0);
        List<String> modeSel = modeDropdown.getSelectedValues();
        String modeLabel = modeSel.isEmpty() ? labelBaniraMode : modeSel.get(0);
        String modeValue = labelVanillaMode.equals(modeLabel)
                ? CustomConfig.notificationReceiveModeVanillaMessage
                : CustomConfig.notificationReceiveModeNotification;

        try {
            PacketUtils.sendPacketToServer(new CustomPlayerConfigSyncToServer(lang, modeValue));
            CustomConfig.setPlayerLanguageClient(uuid, lang);
            CustomConfig.setPlayerNotificationReceiveModeClient(uuid, modeValue);
        } catch (Exception ex) {
            Notification err = Notification.ofComponent(
                    BaniraComponent.get().transClientAuto("custom_player_config_sync_failed",
                            ex.getMessage() != null ? ex.getMessage() : ""));
            err.position(EnumPosition.TOP_RIGHT).durationTime(4000);
            NotificationManager.get().addNotification(err);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void onRender(PoseStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        int cardBg = ColorUtils.applyAlphaToArgb(theme.color(BG_SURFACE), 0xFF);
        AbstractGuiUtils.drawRoundedRect(stack, CARD_MARGIN, CARD_MARGIN, width - CARD_MARGIN * 2, height - CARD_MARGIN * 2,
                CARD_RADIUS, CARD_RADIUS, CARD_RADIUS, CARD_RADIUS, cardBg);
        super.renderWidgets(stack, partialTicks);
    }
}
