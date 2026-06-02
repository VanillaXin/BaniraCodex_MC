package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.TooltipWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.util.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class ConfigEditorActionBar {
    public static final int BUTTON_HEIGHT = 18;
    public static final int CARD_GAP = 1;

    private static final int BUTTON_PADDING = 12;
    private static final int BUTTON_GAP = 8;
    private static final int CARD_RADIUS = 8;
    private static final int CARD_ALPHA = 0xFF;

    private final BaniraScreen screen;
    private final ConfigHolder holder;
    private final Runnable saveAction;
    private final Runnable fetchAction;
    private final Runnable syncAction;
    private final Runnable fullSyncAction;
    private final Runnable closeAction;

    @Getter
    @Accessors(fluent = true)
    private final List<ButtonWidget> buttons = new ArrayList<>();

    public ConfigEditorActionBar(BaniraScreen screen, ConfigHolder holder, Runnable saveAction, Runnable fetchAction,
                                 Runnable syncAction, Runnable fullSyncAction, Runnable closeAction) {
        this.screen = screen;
        this.holder = holder;
        this.saveAction = saveAction;
        this.fetchAction = fetchAction;
        this.syncAction = syncAction;
        this.fullSyncAction = fullSyncAction;
        this.closeAction = closeAction;
    }

    public void rebuildButtons() {
        buttons.clear();
        buttons.add(saveButton());
        if (holder.canSyncToServer()) {
            buttons.add(syncButton());
        }
        buttons.add(closeButton());
    }

    public int maxScrollableHeight(int cardHeight, int cardInner) {
        return Math.max(0, cardHeight - cardInner * 2 - BUTTON_HEIGHT - CARD_GAP);
    }

    public void layout(int cardX, int cardY, int cardW, int cardH, int cardInner,
                       ToIntFunction<ButtonWidget> textWidth) {
        int btnAreaH = BUTTON_HEIGHT + cardInner;
        int btnAreaTop = cardY + cardH - btnAreaH;
        int btnY = btnAreaTop + (btnAreaH - BUTTON_HEIGHT) / 2;
        int n = buttons.size();
        int[] btnWidths = new int[n];
        for (int i = 0; i < n; i++) {
            btnWidths[i] = textWidth.applyAsInt(buttons.get(i)) + BUTTON_PADDING * 2;
        }

        if (n == 3) {
            layoutThreeButtons(cardX, cardW, cardInner, btnY, btnWidths);
        } else {
            layoutSplitButtons(cardX, cardW, cardInner, btnY, btnWidths);
        }

        syncTooltipBounds();
    }

    public void renderChrome(MatrixStack stack, BaniraColorConfig theme, int cardX, int cardY, int cardW, int cardH,
                             int cardInner) {
        int cardBg = ColorUtils.applyAlphaToArgb(theme.bgSurface(), CARD_ALPHA);
        int btnAreaH = BUTTON_HEIGHT + cardInner;
        int btnAreaTop = cardY + cardH - btnAreaH;
        int contentH = btnAreaTop - cardY - CARD_GAP;
        int n = buttons.size();

        AbstractGuiUtils.drawRoundedRect(stack, cardX, cardY, cardW, contentH,
                CARD_RADIUS, CARD_RADIUS, 0, 0, cardBg);

        if (n == 3) {
            int btnTotal = cardW - 2 * CARD_GAP;
            int segW = btnTotal / 3;
            int seg3W = segW + btnTotal % 3;
            AbstractGuiUtils.drawRoundedRect(stack, cardX, btnAreaTop, segW, btnAreaH,
                    0, 0, CARD_RADIUS, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + segW + CARD_GAP, btnAreaTop, segW, btnAreaH,
                    0, 0, 0, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + 2 * (segW + CARD_GAP), btnAreaTop, seg3W, btnAreaH,
                    0, 0, 0, CARD_RADIUS, cardBg);
        } else {
            int contentTotal = cardW - cardInner * 2 - CARD_GAP;
            int zoneW = contentTotal / 2;
            int leftRectW = cardInner + zoneW;
            int rightRectW = cardW - leftRectW - CARD_GAP;
            AbstractGuiUtils.drawRoundedRect(stack, cardX, btnAreaTop, leftRectW, btnAreaH,
                    0, 0, CARD_RADIUS, 0, cardBg);
            AbstractGuiUtils.drawRoundedRect(stack, cardX + leftRectW + CARD_GAP, btnAreaTop, rightRectW, btnAreaH,
                    0, 0, 0, CARD_RADIUS, cardBg);
        }
    }

    public void renderButtons(MatrixStack stack, float partialTicks) {
        for (ButtonWidget btn : buttons) {
            if (btn.visible()) {
                if (btn.enabled() && btn.needsUpdate()) {
                    btn.update();
                }
                btn.render(stack, partialTicks);
            }
        }
    }

    public boolean contains(IWidget widget) {
        return buttons.contains(widget);
    }

    private ButtonWidget saveButton() {
        ButtonWidget saveBtn = new ButtonWidget(screen);
        saveBtn.id("save");
        saveBtn.text(BaniraComponent.get().transClientAuto("config_editor_save").toString());
        saveBtn.onClick(b -> saveAction.run());
        if (holder.canSyncToServer()) {
            saveBtn.onLongPress(b -> fetchAction.run());
        }
        addTooltip(saveBtn, holder.canSyncToServer()
                ? "config_editor_save_tooltip_network"
                : "config_editor_save_tooltip");
        return saveBtn;
    }

    private ButtonWidget syncButton() {
        ButtonWidget syncBtn = new ButtonWidget(screen);
        syncBtn.id("sync");
        syncBtn.text(BaniraComponent.get().transClientAuto("config_editor_sync").toString());
        syncBtn.onClick(b -> syncAction.run());
        syncBtn.onLongPress(b -> fullSyncAction.run());
        addTooltip(syncBtn, "config_editor_sync_tooltip");
        return syncBtn;
    }

    private ButtonWidget closeButton() {
        ButtonWidget closeBtn = new ButtonWidget(screen);
        closeBtn.id("close");
        closeBtn.text(BaniraComponent.get().transClientAuto("config_editor_close").toString());
        closeBtn.onClick(b -> closeAction.run());
        return closeBtn;
    }

    private void addTooltip(ButtonWidget button, String translationKey) {
        TooltipWidget tip = new TooltipWidget(screen, new ScreenCoordinate(0, 0, 20, BUTTON_HEIGHT));
        tip.text(BaniraComponent.get().transClientAuto(translationKey));
        tip.popupAtScreenCoords(true);
        button.addChild(tip);
    }

    private void layoutThreeButtons(int cardX, int cardW, int cardInner, int btnY, int[] btnWidths) {
        int btnTotal = cardW - 2 * CARD_GAP;
        int segW = btnTotal / 3;
        int seg3W = segW + btnTotal % 3;
        int contentW0 = segW - cardInner * 2;
        int contentW2 = seg3W - cardInner * 2;
        for (int i = 0; i < buttons.size(); i++) {
            ButtonWidget btn = buttons.get(i);
            int contentW = i == 2 ? contentW2 : contentW0;
            int bw = Math.min(btnWidths[i], Math.max(20, contentW));
            int segX = cardX + (i == 0 ? 0 : i == 1 ? segW + CARD_GAP : 2 * (segW + CARD_GAP));
            int cx = segX + cardInner + Math.max(0, (contentW - bw) / 2);
            btn.bounds(new ScreenCoordinate(cx, btnY, bw, BUTTON_HEIGHT));
        }
    }

    private void layoutSplitButtons(int cardX, int cardW, int cardInner, int btnY, int[] btnWidths) {
        int n = buttons.size();
        int contentTotal = cardW - cardInner * 2 - CARD_GAP;
        int zoneW = contentTotal / 2;
        int leftRectW = cardInner + zoneW;
        int rightRectW = cardW - leftRectW - CARD_GAP;
        int rightRectX = cardX + leftRectW + CARD_GAP;
        int lastIdx = n - 1;
        int leftTotalW = 0;
        for (int i = 0; i < lastIdx; i++) {
            leftTotalW += btnWidths[i] + (i > 0 ? BUTTON_GAP : 0);
        }
        int rightTotalW = btnWidths[lastIdx];
        double leftScale = leftTotalW > zoneW ? (double) zoneW / leftTotalW : 1.0;
        double rightScale = rightTotalW > zoneW ? (double) zoneW / rightTotalW : 1.0;
        int leftTotalScaled = (int) (leftTotalW * leftScale);
        int curX = cardX + (leftRectW - leftTotalScaled) / 2;
        for (int i = 0; i < n; i++) {
            ButtonWidget btn = buttons.get(i);
            double scale = i < lastIdx ? leftScale : rightScale;
            int bw = Math.max(20, (int) (btnWidths[i] * scale));
            if (i == lastIdx) {
                curX = rightRectX + (rightRectW - bw) / 2;
            }
            btn.bounds(new ScreenCoordinate(curX, btnY, bw, BUTTON_HEIGHT));
            curX += bw + BUTTON_GAP;
        }
    }

    private void syncTooltipBounds() {
        for (ButtonWidget btn : buttons) {
            TooltipWidget tip = btn.findChildByType(TooltipWidget.class);
            if (tip != null && btn.bounds() != null) {
                ScreenCoordinate bc = btn.bounds();
                tip.bounds(new ScreenCoordinate(0, 0, bc.width(), bc.height()));
            }
        }
    }
}
