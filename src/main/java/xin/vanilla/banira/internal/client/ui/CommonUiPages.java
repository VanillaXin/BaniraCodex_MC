package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap;

public final class CommonUiPages {
    private CommonUiPages() { }

    public static void tick() {
        Object current = BaniraClientRuntime.currentScreen();
        UiSessionHost sessions = BaniraCodexClientBootstrap.uiSessions();
        sessions.observeScreen(current);
        if (!(current instanceof UiNativePage) || sessions.ownsScreen(current)) return;
        UiNativePage nativePage = (UiNativePage) current;
        if (!BaniraCodexClientBootstrap.uiBackend().wantsAui(nativePage.uiSurfaceId())) return;
        UiPageSpec page = nativePage.uiPage();
        if (page != null) {
            Object previous = current instanceof xin.vanilla.banira.client.gui.BaniraScreen
                    ? ((xin.vanilla.banira.client.gui.BaniraScreen) current).previousScreen() : null;
            UiViewState state = new UiViewState();
            nativePage.captureUiState(state);
            sessions.open(page, previous, state);
        }
    }

    public static void removed(Object screen) { BaniraCodexClientBootstrap.uiSessions().screenRemoved(screen); }

    public static boolean close(Object screen, UiCloseReason reason) {
        UiSessionHost host = BaniraCodexClientBootstrap.uiSessions();
        if (!host.ownsScreen(screen)) return false;
        host.close(reason);
        return true;
    }
}
