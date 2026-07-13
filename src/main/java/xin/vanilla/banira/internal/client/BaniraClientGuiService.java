package xin.vanilla.banira.internal.client;

import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.api.client.event.BaniraKeyboardEvent;
import xin.vanilla.banira.api.client.event.BaniraMouseEvent;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.client.util.NotificationManager;

/**
 * Banira-owned client GUI behavior. Loader bridges should translate events first,
 * then delegate here for Codex features.
 */
public final class BaniraClientGuiService {

    private BaniraClientGuiService() {
    }

    public static void handleClientTickEnd(boolean noScreenOpen) {
        NotificationManager.get().tickOutOfScreenClick();
        InputStateManager.handleClientTickEnd(noScreenOpen);
        NotificationLogScreen.openHotkeyScreenIfPressed();
    }

    public static void handleScreenOpened() {
        QuickActionOverlay.get().resetInteractionState();
    }

    public static void handleScreenPreRender(Screen screen, double mouseX, double mouseY) {
        InputStateManager.handleDrawScreenPre(mouseX, mouseY);
        if (screen != null && QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().tickInteraction(screen, (int) mouseX, (int) mouseY);
        }
    }

    public static boolean handleMouseClicked(Screen screen, BaniraMouseEvent event) {
        InputStateManager.handleMouseClicked(event.mouseX(), event.mouseY(), event.button());
        if (screen != null && QuickActionOverlay.get().handleMouseClicked(screen, event.mouseX(), event.mouseY(), event.button())) {
            return true;
        }
        return NotificationManager.get().tryHandleHudClick(event.mouseX(), event.mouseY(), event.button());
    }

    public static boolean handleMouseReleased(Screen screen, BaniraMouseEvent event) {
        InputStateManager.handleMouseReleased(event.mouseX(), event.mouseY(), event.button());
        return screen != null && QuickActionOverlay.get().handleMouseReleased(screen, event.mouseX(), event.mouseY(), event.button());
    }

    public static boolean handleMouseScrolled(Screen screen, BaniraMouseEvent event) {
        InputStateManager.handleMouseScrolled(event.mouseX(), event.mouseY(), event.scrollDelta());
        return screen != null && QuickActionOverlay.get().handleMouseScroll(screen, event.mouseX(), event.mouseY(), event.scrollDelta());
    }

    public static void handleKeyboard(BaniraKeyboardEvent event) {
        if (event.action() == BaniraKeyboardEvent.Action.KEY_PRESS) {
            InputStateManager.handleKeyPressed(event.keyCode());
        } else if (event.action() == BaniraKeyboardEvent.Action.KEY_RELEASE) {
            InputStateManager.handleKeyReleased(event.keyCode());
        }
    }
}
