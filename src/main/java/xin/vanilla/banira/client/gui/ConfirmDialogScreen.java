package xin.vanilla.banira.client.gui;

import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.internal.client.ui.CommonUiPages;
import xin.vanilla.banira.internal.client.ui.UiNativePage;
import xin.vanilla.banira.internal.client.ui.page.DialogUiSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 通用确认界面，用明确的确认与取消按钮承接破坏性操作。
 */
public final class ConfirmDialogScreen extends BaniraScreen implements UiNativePage {

    private static final int CARD_PADDING = 12;
    private static final int CARD_RADIUS = 6;
    private static final int CARD_MAX_WIDTH = 360;
    private static final int CARD_HEIGHT = 116;
    private static final int TITLE_HEIGHT = 18;
    private static final int MESSAGE_GAP = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 8;

    private final Args args;
    private final DialogUiSession uiSession;
    private UiPageSpec uiPage;

    public ConfirmDialogScreen(@Nonnull Args args) {
        super(args.title() != null ? args.title() : BaniraComponent.get().literal("Confirm"));
        this.args = args;
        this.uiSession = new DialogUiSession(args);
        previousScreen(args.parentScreen());
        BaniraScreen.inheritThemeAndSeason(this, args.parentScreen(), args.theme(), args.season());
    }

    public DialogUiSession uiSession() {
        return uiSession;
    }

    @Override
    public String uiSurfaceId() {
        return "banira_codex:confirm";
    }

    @Override
    public UiPageSpec uiPage() {
        if (uiPage == null) uiPage = new UiPageSpec(uiSurfaceId(),
                "banira_codex/ui/confirm.html", uiSession, previous -> this, this::getEffectiveTheme);
        return uiPage;
    }

    /**
     * 确认界面的内容与行为参数。
     */
    @Data
    @Accessors(chain = true, fluent = true)
    public static final class Args {
        @Nullable
        private Screen parentScreen;
        @Nullable
        private BaniraColorConfig theme;
        @Nullable
        private EnumSeason season;
        @Nullable
        private Component title;
        @Nullable
        private Component message;
        @Nullable
        private Component confirmText;
        @Nullable
        private Component cancelText;
        @Nullable
        private Runnable onConfirm;
        @Nullable
        private Runnable onCancel;
    }

    @Override
    protected void initWidgets() {
        int cardWidth = Math.max(180, Math.min(CARD_MAX_WIDTH, width - 32));
        int cardX = (width - cardWidth) / 2;
        int cardY = (height - CARD_HEIGHT) / 2;
        int contentWidth = cardWidth - CARD_PADDING * 2;

        LabelWidget title = new LabelWidget(this);
        title.id("title");
        title.bounds(new ScreenCoordinate(cardX + CARD_PADDING, cardY + CARD_PADDING,
                contentWidth, TITLE_HEIGHT));
        title.text(args.title() != null ? args.title() : BaniraComponent.get().literal(""));
        title.textWrap(false);
        title.textVerticalAlign(EnumAlignment.CENTER);
        addWidget(title);

        LabelWidget message = new LabelWidget(this);
        message.id("message");
        message.bounds(new ScreenCoordinate(cardX + CARD_PADDING,
                cardY + CARD_PADDING + TITLE_HEIGHT + MESSAGE_GAP,
                contentWidth, CARD_HEIGHT - CARD_PADDING * 3 - TITLE_HEIGHT - MESSAGE_GAP - BUTTON_HEIGHT));
        message.text(args.message() != null ? args.message() : BaniraComponent.get().literal(""));
        message.textWrap(true);
        addWidget(message);

        int buttonWidth = (contentWidth - BUTTON_GAP) / 2;
        int buttonY = cardY + CARD_HEIGHT - CARD_PADDING - BUTTON_HEIGHT;

        ButtonWidget cancel = new ButtonWidget(this);
        cancel.id("cancel");
        cancel.bounds(new ScreenCoordinate(cardX + CARD_PADDING, buttonY, buttonWidth, BUTTON_HEIGHT));
        cancel.text(args.cancelText() != null
                ? args.cancelText()
                : BaniraComponent.get().transClientAuto("cancel"));
        cancel.onClick(button -> cancel());
        addWidget(cancel);

        ButtonWidget confirm = new ButtonWidget(this);
        confirm.id("confirm");
        confirm.bounds(new ScreenCoordinate(cardX + CARD_PADDING + buttonWidth + BUTTON_GAP,
                buttonY, buttonWidth, BUTTON_HEIGHT));
        confirm.text(args.confirmText() != null
                ? args.confirmText()
                : BaniraComponent.get().transClientAuto("confirm"));
        confirm.onClick(button -> confirm());
        addWidget(confirm);
    }

    @Override
    protected void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, width, height, 0x55000000);
        int cardWidth = Math.max(180, Math.min(CARD_MAX_WIDTH, width - 32));
        int cardX = (width - cardWidth) / 2;
        int cardY = (height - CARD_HEIGHT) / 2;
        int cardColor = ColorUtils.applyAlphaToArgb(getEffectiveTheme().bgSurface(), 0xFF);
        AbstractGuiUtils.drawRoundedRect(graphics.pose(), cardX, cardY, cardWidth, CARD_HEIGHT,
                CARD_RADIUS, CARD_RADIUS, CARD_RADIUS, CARD_RADIUS, cardColor);
        renderWidgets(graphics, partialTicks);
    }

    private void confirm() {
        if (uiSession.resolved()) {
            return;
        }
        uiSession.confirm();
        returnToParent(UiCloseReason.SUBMIT);
    }

    private void cancel() {
        if (uiSession.resolved()) {
            return;
        }
        uiSession.cancel();
        returnToParent(UiCloseReason.CANCEL);
    }

    private void returnToParent(UiCloseReason reason) {
        if (CommonUiPages.close(this, reason)) return;
        uiSession.close(reason);
        Minecraft.getInstance().setScreen(args.parentScreen());
    }

    @Override
    public void removed() {
        CommonUiPages.removed(this);
        super.removed();
    }

    @Override
    public void onClose() {
        cancel();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
