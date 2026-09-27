package xin.vanilla.banira.client.gui;

import xin.vanilla.banira.api.BaniraConfigs;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.quickaction.CustomQuickActionConfigScreen;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.internal.DebugScreen;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;

import javax.annotation.Nullable;

/**
 * 香草志功能导航
 */
public class CodexNavigationScreen extends BaniraScreen {

    private static final int CARD_MARGIN = 16;
    private static final int BTN_H = 22;
    private static final int BTN_GAP = 10;
    private static final int CARD_RADIUS = 8;
    private static final float CLOSE_BTN_SIZE = 10f;
    private static final float CLOSE_BTN_PAD = 6f;

    private final Args args;

    public CodexNavigationScreen(@Nullable Args args) {
        super(BaniraComponent.get().transClientAuto("codex_navigation_title").toVanilla());
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
        int innerH = height - CARD_MARGIN * 2;
        int btnW = Math.min(340, width - CARD_MARGIN * 2);
        int cx = (width - btnW) / 2;
        int gap = Math.max(2, Math.min(BTN_GAP, (innerH - 6 * BTN_H) / 5));
        int contentH = 6 * BTN_H + 5 * gap;
        int y = CARD_MARGIN + Math.max(0, (innerH - contentH) / 2);

        addNavButton(cx, y, btnW, "codex_navigation_notification_log",
                () -> Minecraft.getInstance().setScreen(new NotificationLogScreen(new NotificationLogScreen.Args().parentScreen(this))));
        y += BTN_H + gap;

        addNavButton(cx, y, btnW, "codex_navigation_client_config",
                () -> ConfigEditorScreen.open(BaniraConfigs.holder(ClientConfig.class), this));
        y += BTN_H + gap;

        addNavButton(cx, y, btnW, "codex_navigation_common_config",
                () -> ConfigEditorScreen.open(BaniraConfigs.holder(CommonConfig.class), this));
        y += BTN_H + gap;

        addNavButton(cx, y, btnW, "custom_player_config_title",
                () -> Minecraft.getInstance().setScreen(new CustomPlayerConfigEditScreen(new CustomPlayerConfigEditScreen.Args().parentScreen(this))));
        y += BTN_H + gap;

        addNavButton(cx, y, btnW, "custom_quick_action_title",
                () -> Minecraft.getInstance().setScreen(new CustomQuickActionConfigScreen(this)));

        y += BTN_H + gap;
        addNavButton(cx, y, btnW, "notification_hud_position",
                () -> Minecraft.getInstance().setScreen(new NotificationHudPositionScreen(this)));

        ButtonWidget closeBtn = new ButtonWidget(this);
        closeBtn.id("close");
        closeBtn.bounds(new ScreenCoordinate(width - CARD_MARGIN - CLOSE_BTN_PAD - CLOSE_BTN_SIZE, CARD_MARGIN + CLOSE_BTN_PAD, CLOSE_BTN_SIZE, CLOSE_BTN_SIZE));
        closeBtn.presetStyleClose();
        closeBtn.radius(CLOSE_BTN_SIZE / 3f);
        closeBtn.padding(1);
        closeBtn.onClick(b -> onClose());
        closeBtn.onLongPress(b -> Minecraft.getInstance().setScreen(new DebugScreen().previousScreen(this)));
        addWidget(closeBtn);
    }

    private void addNavButton(int x, int y, int w, String langKey, Runnable action) {
        ButtonWidget btn = new ButtonWidget(this);
        btn.id(langKey);
        btn.bounds(new ScreenCoordinate(x, y, w, BTN_H));
        btn.text(BaniraComponent.get().transClientAuto(langKey).toString());
        btn.onClick(b -> action.run());
        addWidget(btn);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected ScreenCoordinate closeableWindowBounds() {
        return new ScreenCoordinate(CARD_MARGIN, CARD_MARGIN,
                width - CARD_MARGIN * 2, height - CARD_MARGIN * 2);
    }

    @Override
    protected void onRender(MatrixStack stack, float partialTicks) {
        BaniraColorConfig theme = getEffectiveTheme();
        int cardBg = ColorUtils.applyAlphaToArgb(theme.bgSurface(), 0xFF);
        AbstractGuiUtils.drawRoundedRect(stack, CARD_MARGIN, CARD_MARGIN, width - CARD_MARGIN * 2, height - CARD_MARGIN * 2,
                CARD_RADIUS, CARD_RADIUS, CARD_RADIUS, CARD_RADIUS, cardBg);
        super.renderWidgets(stack, partialTicks);
    }
}
