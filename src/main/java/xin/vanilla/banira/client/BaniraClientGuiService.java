package xin.vanilla.banira.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.client.event.BaniraClientInputEvent;
import xin.vanilla.banira.client.event.BaniraClientInputEventType;
import xin.vanilla.banira.client.event.BaniraClientScreenEvent;
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

    public static void handleScreenOpened(BaniraClientScreenEvent event) {
        QuickActionOverlay.get().resetInteractionState();
    }

    public static void handleScreenPreRender(BaniraClientScreenEvent event) {
        InputStateManager.handleDrawScreenPre(event.mouseX(), event.mouseY());
        Screen screen = event.nativeScreen(Screen.class);
        if (screen != null && QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().tickInteraction(screen, (int) event.mouseX(), (int) event.mouseY());
        }
    }

    public static void handleScreenPostRenderLowest(BaniraClientScreenEvent event) {
        Screen screen = event.nativeScreen(Screen.class);
        MatrixStack stack = event.draw() == null ? null : event.draw().nativeContext(MatrixStack.class);
        if (screen != null && stack != null && QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().render(stack, screen, (int) event.mouseX(), (int) event.mouseY(), event.partialTicks());
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
    }

    public static boolean handleInput(BaniraClientInputEvent event) {
        if (event == null || event.type() == null) {
            return false;
        }
        if (event.type() == BaniraClientInputEventType.MOUSE_CLICK) {
            return handleMouseClicked(event);
        }
        if (event.type() == BaniraClientInputEventType.MOUSE_RELEASE) {
            return handleMouseReleased(event);
        }
        if (event.type() == BaniraClientInputEventType.MOUSE_SCROLL) {
            return handleMouseScrolled(event);
        }
        if (event.type() == BaniraClientInputEventType.KEY_PRESS) {
            InputStateManager.handleKeyPressed(event.keyCode());
            return false;
        }
        if (event.type() == BaniraClientInputEventType.KEY_RELEASE) {
            InputStateManager.handleKeyReleased(event.keyCode());
            return false;
        }
        return false;
    }

    private static boolean handleMouseClicked(BaniraClientInputEvent event) {
        InputStateManager.handleMouseClicked(event.mouseX(), event.mouseY(), event.button());
        Screen screen = event.nativeScreen(Screen.class);
        if (screen != null && QuickActionOverlay.get().handleMouseClicked(screen, event.mouseX(), event.mouseY(), event.button())) {
            return true;
        }
        return NotificationManager.get().tryHandleHudClick(event.mouseX(), event.mouseY(), event.button());
    }

    private static boolean handleMouseReleased(BaniraClientInputEvent event) {
        InputStateManager.handleMouseReleased(event.mouseX(), event.mouseY(), event.button());
        Screen screen = event.nativeScreen(Screen.class);
        return screen != null && QuickActionOverlay.get().handleMouseReleased(screen, event.mouseX(), event.mouseY(), event.button());
    }

    private static boolean handleMouseScrolled(BaniraClientInputEvent event) {
        InputStateManager.handleMouseScrolled(event.mouseX(), event.mouseY(), event.scrollDelta());
        Screen screen = event.nativeScreen(Screen.class);
        return screen != null && QuickActionOverlay.get().handleMouseScroll(screen, event.mouseX(), event.mouseY(), event.scrollDelta());
    }
}
