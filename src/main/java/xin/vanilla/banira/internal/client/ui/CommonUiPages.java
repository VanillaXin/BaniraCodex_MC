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
        if (sessions.isReturning(current)) return;
        if (!(current instanceof xin.vanilla.banira.api.client.ui.UiPageSource) || sessions.ownsScreen(current)) return;
        xin.vanilla.banira.api.client.ui.UiPageSource nativePage = (xin.vanilla.banira.api.client.ui.UiPageSource) current;
        boolean aui = BaniraCodexClientBootstrap.uiBackend().wantsAui(nativePage.uiSurfaceId());
        if (!aui && !(current instanceof xin.vanilla.banira.client.gui.ConfigEditorScreen)) return;
        UiPageSpec page = nativePage.uiPage();
        if (page != null) {
            Object previous = current instanceof xin.vanilla.banira.client.gui.BaniraScreen
                    ? ((xin.vanilla.banira.client.gui.BaniraScreen) current).previousScreen() : null;
            UiViewState state = new UiViewState();
            nativePage.capturePageState(state);
            if (aui) sessions.open(page, previous, state);
            else sessions.adoptNative(page, current, previous, state);
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
