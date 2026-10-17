package xin.vanilla.banira.api.client.ui;

import java.util.Objects;

public final class BaniraUi {
    public interface Driver {
        void open(UiPageSpec page, Object previousScreen);
        boolean usesAui(String surfaceId);
        AutoCloseable registerHud(UiHudSpec hud);
    }
    private static volatile Driver driver;

    private BaniraUi() { }

    public static void install(Driver clientDriver) {
        driver = Objects.requireNonNull(clientDriver, "clientDriver");
    }

    public static void open(UiPageSpec page, Object previousScreen) {
        Driver current = driver;
        if (current == null) throw new IllegalStateException("Client UI is not initialized");
        current.open(Objects.requireNonNull(page, "page"), previousScreen);
    }

    public static boolean usesAui(String surfaceId) {
        Driver current = driver;
        return current != null && current.usesAui(surfaceId);
    }

    public static AutoCloseable registerHud(UiHudSpec hud) {
        Driver current = driver;
        if (current == null) throw new IllegalStateException("Client UI is not initialized");
        return current.registerHud(Objects.requireNonNull(hud, "hud"));
    }
}
