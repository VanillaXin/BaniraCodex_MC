package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.notification.NotificationHudState;
import xin.vanilla.banira.client.notification.NotificationUnreadHud;
import xin.vanilla.banira.client.notification.NotificationStyleInteractionHelper;
import xin.vanilla.banira.internal.client.InputStateManager;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.config.ClientConfig;
import java.util.ArrayList;
import java.util.List;

public final class NotificationHudPositionScreen extends BaniraScreen {
    private final double originalX, originalY;
    private double x, y, grabX, grabY;
    private boolean dragging;
    private final List<ButtonWidget> controls = new ArrayList<>();

    public NotificationHudPositionScreen(Screen parent) {
        super(BaniraComponent.get().transClientAuto("notification_hud_position").toVanilla());
        previousScreen(parent);
        BaniraScreen.inheritThemeAndSeason(this, parent, null, null);
        x = originalX = ClientConfig.get().notificationHud().x();
        y = originalY = ClientConfig.get().notificationHud().y();
    }

    @Override
    protected void initWidgets() {
        dragging = false;
        controls.clear();
        int w = Math.min(100, (width - 32) / 3);
        addButton("notification_hud_settings", width / 2 - w * 3 / 2 - 4, w,
                () -> ConfigEditorScreen.open(BaniraConfigs.holder(ClientConfig.class), this));
        addButton("save", width / 2 - w / 2, w, () -> {
            ClientConfig.get().notificationHud().x(x).y(y);
            BaniraConfigs.save(ClientConfig.class);
            onClose();
        });
        addButton("cancel", width / 2 + w / 2 + 4, w, this::onClose);
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

    private int toolbarY() { return y > .5 ? 10 : height - 30; }

    private int pixelX() { return NotificationHudState.position(x, width, NotificationUnreadHud.WIDTH); }
    private int pixelY() { return NotificationHudState.position(y, height, NotificationUnreadHud.HEIGHT); }
    private boolean hovered(double mx, double my) {
        return mx >= pixelX() && mx < pixelX() + NotificationUnreadHud.WIDTH
                && my >= pixelY() && my < pixelY() + NotificationUnreadHud.HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && hovered(mx, my)) {
            dragging = true; grabX = mx - pixelX(); grabY = my - pixelY();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && button == 0) {
            x = NotificationHudState.relative(mx - grabX, width, NotificationUnreadHud.WIDTH);
            y = NotificationHudState.relative(my - grabY, height, NotificationUnreadHud.HEIGHT);
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
    public boolean shouldCloseOnEsc() { return x == originalX && y == originalY; }

    @Override
    protected boolean requestClose(CloseReason reason) {
        if (shouldCloseOnEsc()) onClose();
        return true;
    }

    @Override
    protected void onRender(PoseStack stack, float partialTicks) {
        for (ButtonWidget control : controls) {
            if (control.bounds().y() != toolbarY()) {
                control.bounds(new ScreenCoordinate(control.bounds().x(), toolbarY(),
                        control.bounds().width(), control.bounds().height()));
            }
        }
        NotificationUnreadHud.draw(stack, pixelX(), pixelY(), 100, getEffectiveTheme());
        renderWidgets(stack, partialTicks);
        KeyValue<Integer, Integer> mouse = InputStateManager.getGuiCursorPos();
        if (!dragging && hovered(mouse.key(), mouse.val())) {
            NotificationStyleInteractionHelper.renderTextTooltip(stack, mouse.key(), mouse.val(),
                    BaniraComponent.get().transClientAuto("notification_hud_drag"), getEffectiveTheme());
        }
    }
}
