package xin.vanilla.banira.api.client.ui;

import java.util.Objects;

public final class BaniraUi {
    public interface Driver {
        void open(UiPageSpec page, Object previousScreen);

        boolean usesAui(String surfaceId);

        AutoCloseable registerHud(UiHudSpec hud);

        default UiContainerLayer container(UiPageSpec page, java.util.function.BooleanSupplier current) {
            return null;
        }

        default boolean renderHud(String id, com.mojang.blaze3d.vertex.PoseStack graphics) {
            return false;
        }

        default void screenRemoved(Object screen) {
        }

        default boolean closePage(Object screen, UiCloseReason reason) {
            return false;
        }
    }

    private static volatile Driver driver;

    private BaniraUi() {
    }

    public static void screenRemoved(Object screen) {
        Driver current = driver;
        if (current != null) current.screenRemoved(screen);
    }

    public static boolean closePage(Object screen, UiCloseReason reason) {
        Driver current = driver;
        return current != null && current.closePage(screen, Objects.requireNonNull(reason));
    }

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

    public static UiContainerLayer container(UiPageSpec page, java.util.function.BooleanSupplier current) {
        Driver installed = driver;
        return installed == null ? null : installed.container(Objects.requireNonNull(page), Objects.requireNonNull(current));
    }

    public static boolean renderHud(String id, com.mojang.blaze3d.vertex.PoseStack graphics) {
        Driver installed = driver;
        return installed != null && installed.renderHud(Objects.requireNonNull(id), Objects.requireNonNull(graphics));
    }
}
