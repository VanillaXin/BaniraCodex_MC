package xin.vanilla.banira.api.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BaniraUi {
    public interface Driver {
        void open(UiPageSpec page, Object previousScreen);

        boolean usesAui(String surfaceId);

        AutoCloseable registerHud(UiHudSpec hud);

        default UiContainerLayer container(UiPageSpec page, java.util.function.BooleanSupplier current) {
            return null;
        }

        default boolean renderHud(String id, net.minecraft.client.gui.GuiGraphics graphics) {
            return false;
        }

        default void screenRemoved(Object screen) {
        }

        default boolean closePage(Object screen, UiCloseReason reason) {
            return false;
        }
    }

    private static volatile Driver driver;
    private static final List<PendingHud> pendingHuds = new ArrayList<>();

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

    public static synchronized void install(Driver clientDriver) {
        driver = Objects.requireNonNull(clientDriver, "clientDriver");
        for (PendingHud pending : new ArrayList<>(pendingHuds)) {
            pending.attach(clientDriver);
            pendingHuds.remove(pending);
        }
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

    public static synchronized AutoCloseable registerHud(UiHudSpec hud) {
        Objects.requireNonNull(hud, "hud");
        Driver current = driver;
        if (current != null) return current.registerHud(hud);
        // Dependent Fabric entrypoints can run before Banira's client entrypoint.
        PendingHud pending = new PendingHud(hud);
        pendingHuds.add(pending);
        return pending;
    }

    private static final class PendingHud implements AutoCloseable {
        private final UiHudSpec spec;
        private AutoCloseable registration;
        private boolean closed;

        private PendingHud(UiHudSpec spec) {
            this.spec = spec;
        }

        private void attach(Driver clientDriver) {
            if (closed) return;
            AutoCloseable attached = Objects.requireNonNull(clientDriver.registerHud(spec), "HUD registration");
            if (closed) {
                try {
                    attached.close();
                } catch (Exception failure) {
                    throw new IllegalStateException("Unable to close cancelled HUD registration", failure);
                }
            } else {
                registration = attached;
            }
        }

        @Override
        public void close() throws Exception {
            synchronized (BaniraUi.class) {
                if (closed) return;
                closed = true;
                pendingHuds.remove(this);
                if (registration != null) registration.close();
            }
        }
    }

    public static UiContainerLayer container(UiPageSpec page, java.util.function.BooleanSupplier current) {
        Driver installed = driver;
        return installed == null ? null : installed.container(Objects.requireNonNull(page), Objects.requireNonNull(current));
    }

    public static boolean renderHud(String id, net.minecraft.client.gui.GuiGraphics graphics) {
        Driver installed = driver;
        return installed != null && installed.renderHud(Objects.requireNonNull(id), Objects.requireNonNull(graphics));
    }
}
