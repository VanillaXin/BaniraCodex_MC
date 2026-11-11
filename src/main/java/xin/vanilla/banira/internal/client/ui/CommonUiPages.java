package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.Minecraft;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap;

public final class CommonUiPages {
    private static final UiNativeSessions NATIVE_SESSIONS = new UiNativeSessions();

    private CommonUiPages() {
    }

    public static void tick() {
        Object current = Minecraft.getInstance().screen;
        UiSessionHost sessions = BaniraCodexClientBootstrap.uiSessions();
        sessions.observeScreen(current);
        if (!(current instanceof xin.vanilla.banira.api.client.ui.UiPageSource)) return;
        xin.vanilla.banira.api.client.ui.UiPageSource nativePage = (xin.vanilla.banira.api.client.ui.UiPageSource) current;
        if (sessions.isReturning(current) || sessions.ownsScreen(current)) {
            NATIVE_SESSIONS.forget(nativePage, true);
            return;
        }
        NATIVE_SESSIONS.remember(nativePage);
        boolean aui = BaniraCodexClientBootstrap.uiBackend().wantsAui(nativePage.uiSurfaceId());
        if (!aui && !(current instanceof xin.vanilla.banira.client.gui.ConfigEditorScreen)) return;
        UiPageSpec page = nativePage.uiPage();
        if (page != null) {
            Object previous = current instanceof xin.vanilla.banira.client.gui.BaniraScreen
                    ? ((xin.vanilla.banira.client.gui.BaniraScreen) current).previousScreen() : null;
            UiViewState state = new UiViewState();
            nativePage.capturePageState(state);
            boolean transferred = aui ? sessions.open(page, previous, state)
                    : sessions.adoptNative(page, current, previous, state);
            NATIVE_SESSIONS.forget(nativePage, transferred);
        }
    }

    public static void removed(Object screen) {
        BaniraCodexClientBootstrap.uiSessions().screenRemoved(screen);
    }

    public static void disconnect() {
        try {
            BaniraCodexClientBootstrap.uiSessions().disconnect();
        } finally {
            NATIVE_SESSIONS.disconnect();
        }
    }

    public static boolean close(Object screen, UiCloseReason reason) {
        UiSessionHost host = BaniraCodexClientBootstrap.uiSessions();
        if (!host.ownsScreen(screen)) return false;
        host.close(reason);
        return true;
    }
}
