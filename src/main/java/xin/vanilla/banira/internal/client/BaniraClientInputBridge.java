package xin.vanilla.banira.internal.client;

import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.api.client.event.BaniraKeyboardEvent;
import xin.vanilla.banira.api.client.event.BaniraMouseEvent;

import javax.annotation.Nonnull;

/**
 * 客户端输入事件桥；加载器适配层只传入原生参数，取消语义统一由 Banira 事件对象承载。
 */
public final class BaniraClientInputBridge {

    private BaniraClientInputBridge() {
    }

    public static boolean allowMouseClick(@Nonnull Screen screen, double mouseX, double mouseY, int button) {
        BaniraMouseEvent event = BaniraMouseEvent.clicked(BaniraClientEventBridge.screenInfo(screen), mouseX, mouseY, button);
        BaniraClientEventHub.dispatchMouseClickedPre(event);
        return !event.canceled() && BaniraClientOverlayBridge.allowMouseClick(screen, mouseX, mouseY, button);
    }

    public static boolean allowMouseRelease(@Nonnull Screen screen, double mouseX, double mouseY, int button) {
        BaniraMouseEvent event = BaniraMouseEvent.released(BaniraClientEventBridge.screenInfo(screen), mouseX, mouseY, button);
        BaniraClientEventHub.dispatchMouseReleasedPre(event);
        BaniraClientEventHub.dispatchMouseReleasedPost(event);
        return !event.canceled() && BaniraClientOverlayBridge.allowMouseRelease(screen, mouseX, mouseY, button);
    }

    public static boolean allowMouseScroll(@Nonnull Screen screen, double mouseX, double mouseY, double verticalAmount) {
        BaniraMouseEvent event = BaniraMouseEvent.scrolled(BaniraClientEventBridge.screenInfo(screen), mouseX, mouseY, verticalAmount);
        BaniraClientEventHub.dispatchMouseScrolledPre(event);
        return !event.canceled() && BaniraClientOverlayBridge.allowMouseScroll(screen, mouseX, mouseY, verticalAmount);
    }

    public static boolean allowKeyPress(@Nonnull Screen screen, int keyCode, int scanCode, int modifiers) {
        BaniraKeyboardEvent event = BaniraKeyboardEvent.pressed(BaniraClientEventBridge.screenInfo(screen), keyCode, scanCode, modifiers);
        BaniraClientEventHub.dispatchKeyPressedPre(event);
        return !event.canceled();
    }

    public static boolean allowKeyRelease(@Nonnull Screen screen, int keyCode, int scanCode, int modifiers) {
        BaniraKeyboardEvent event = BaniraKeyboardEvent.released(BaniraClientEventBridge.screenInfo(screen), keyCode, scanCode, modifiers);
        BaniraClientEventHub.dispatchKeyReleasedPost(event);
        return !event.canceled();
    }
}
