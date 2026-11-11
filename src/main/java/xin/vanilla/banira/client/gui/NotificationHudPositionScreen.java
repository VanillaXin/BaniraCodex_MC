package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.notification.NotificationHudState;
import xin.vanilla.banira.client.notification.NotificationStyleInteractionHelper;
import xin.vanilla.banira.client.notification.NotificationUnreadHud;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.client.ui.CommonUiPages;
import xin.vanilla.banira.internal.client.ui.UiNativePage;
import xin.vanilla.banira.internal.client.ui.page.NotificationUiSession;

import java.util.ArrayList;
import java.util.List;

public final class NotificationHudPositionScreen extends BaniraScreen implements UiNativePage {
    private final NotificationUiSession uiSession;
    private double grabX, grabY;
    private boolean dragging;
    private final List<ButtonWidget> controls = new ArrayList<>();

    public NotificationHudPositionScreen(Screen parent) {
        super(BaniraComponent.get().transClientAuto("notification_hud_position").toVanilla());
        previousScreen(parent);
        BaniraScreen.inheritThemeAndSeason(this, parent, null, null);
        uiSession = NotificationUiSession.position();
        uiSession.attachNative(this);
    }

    @Override
    public NotificationUiSession uiSession() {
        return uiSession;
    }

    @Override
    public String uiSurfaceId() {
        return "banira_codex:hud-position";
    }

    @Override
    public UiPageSpec uiPage() {
        return uiSession.spec();
    }

    @Override
    protected void onRemoved() {
        dragging = false;
        CommonUiPages.removed(this);
        super.onRemoved();
    }

    @Override
    public void onClose() {
        if (CommonUiPages.close(this, UiCloseReason.CANCEL)) return;
        uiSession.close(UiCloseReason.CANCEL);
        super.onClose();
    }

    @Override
    protected void initWidgets() {
        dragging = false;
        controls.clear();
        int w = Math.min(100, (width - 32) / 2);
        addButton("save", width / 2 - w - 2, w, () -> {
            if (uiSession.save()) onClose();
        });
        addButton("cancel", width / 2 + 2, w, this::onClose);
    }

    private void addButton(String key, int bx, int w, Runnable action) {
        ButtonWidget button = new ButtonWidget(this);
        button.id(key);
        button.bounds(new ScreenCoordinate(bx, toolbarY(), w, 20));
        button.text(BaniraComponent.get().transClientAuto(key).toString());
        button.onClick(b -> action.run());
        addWidget(button);
        controls.add(button);
    }

    private int toolbarY() {
        return uiSession.y() > .5 ? 10 : height - 30;
    }

    private int pixelX() {
        return NotificationHudState.position(uiSession.x(), width, NotificationUnreadHud.WIDTH);
    }

    private int pixelY() {
        return NotificationHudState.position(uiSession.y(), height, NotificationUnreadHud.HEIGHT);
    }

    private boolean hovered(double mx, double my) {
        return mx >= pixelX() && mx < pixelX() + NotificationUnreadHud.WIDTH
                && my >= pixelY() && my < pixelY() + NotificationUnreadHud.HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && hovered(mx, my)) {
            dragging = true;
            grabX = mx - pixelX();
            grabY = my - pixelY();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            uiSession.moveTo(NotificationHudState.relative(mx - grabX, width, NotificationUnreadHud.WIDTH),
                    NotificationHudState.relative(my - grabY, height, NotificationUnreadHud.HEIGHT));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean wasDragging = dragging;
        dragging = false;
        return wasDragging || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return uiSession.canClose(UiCloseReason.USER_CLOSE);
    }

    @Override
    protected boolean requestClose(CloseReason reason) {
        if (shouldCloseOnEsc()) onClose();
        return true;
    }

    @Override
    protected void onRender(PoseStack graphics, float partialTicks) {
        int mouseX = (int) inputState.mouseX(), mouseY = (int) inputState.mouseY();
        PoseStack stack = graphics;
        for (ButtonWidget control : controls) {
            if (control.bounds().y() != toolbarY()) {
                control.bounds(new ScreenCoordinate(control.bounds().x(), toolbarY(),
                        control.bounds().width(), control.bounds().height()));
            }
        }
        NotificationUnreadHud.draw(graphics, pixelX(), pixelY(), 100, getEffectiveTheme());
        renderWidgets(graphics, partialTicks);
        KeyValue<Integer, Integer> mouse = InputStateManager.getGuiCursorPos();
        if (!dragging && hovered(mouse.key(), mouse.val())) {
            NotificationStyleInteractionHelper.renderTextTooltip(stack, mouse.key(), mouse.val(),
                    BaniraComponent.get().transClientAuto("notification_hud_drag"), getEffectiveTheme());
        }
    }
}
