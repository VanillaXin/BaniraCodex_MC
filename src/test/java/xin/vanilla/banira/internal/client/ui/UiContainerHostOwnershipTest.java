package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class UiContainerHostOwnershipTest {
    private static final UiPageSession SESSION = new UiPageSession() {
        public void bind(UiView view) {
        }

        public void detach(UiDetachReason reason) {
        }

        public void close(UiCloseReason reason) {
        }
    };

    private UiPageSpec page() {
        return new UiPageSpec("test:container", "test/container.html", SESSION, previous -> previous);
    }

    private Map<String, ScreenCoordinate> regions() {
        return Collections.singletonMap("panel", new ScreenCoordinate(10, 20, 176, 222));
    }

    private static class View extends NativeUiView implements UiContainerLayerHost.Renderer {
        int renders, closes;
        RuntimeException failure;

        View() {
            super(null);
        }

        public void render(net.minecraft.client.gui.GuiGraphics graphics, Map<String, ScreenCoordinate> regions) {
            renders++;
            if (failure != null) throw failure;
        }

        @Override
        public void close() {
            if (!closed()) closes++;
            super.close();
        }
    }

    @Test
    public void existingMenuAppearanceHasAnOptionalPublicFactory() throws Exception {
        try {
            Class<?> layer = Class.forName("xin.vanilla.banira.api.client.ui.UiContainerLayer");
            assertTrue(AutoCloseable.class.isAssignableFrom(layer));
            assertNotNull(xin.vanilla.banira.api.client.ui.BaniraUi.Driver.class.getMethod("container",
                    xin.vanilla.banira.api.client.ui.UiPageSpec.class, java.util.function.BooleanSupplier.class));
        } catch (ClassNotFoundException | NoSuchMethodException missing) {
            fail("Missing optional container appearance factory");
        }
    }

    @Test
    public void absentOrDisabledCapabilityNeverCreatesTheEngine() {
        for (boolean preference : new boolean[]{false, true}) {
            UiBackendController backend = new UiBackendController(() -> preference, () -> false);
            assertNull(UiContainerLayerHost.create(backend, page(), () -> true, () -> {
                throw new AssertionError("Unexpected engine creation");
            }));
        }
    }

    @Test
    public void successfulFrameClaimsOnlyItsOwnSurfaceAndClosesOnce() {
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        View view = new View();
        UiContainerLayer layer = UiContainerLayerHost.create(backend, page(), () -> true, () -> view);
        assertNotNull(layer);
        assertSame(view, layer.view());
        assertFalse(backend.usesAui("test:container"));
        assertTrue(layer.render(null, regions()));
        assertTrue(backend.usesAui("test:container"));
        assertFalse(backend.usesAui("test:other"));
        layer.close();
        layer.close();
        assertEquals(1, view.closes);
        assertFalse(backend.usesAui("test:container"));
    }

    @Test
    public void switchReloadAndLostMenuRejectOldActionsAndReleaseAppearance() {
        for (int mode = 0; mode < 3; mode++) {
            UiBackendController backend = new UiBackendController(() -> true, () -> true);
            AtomicBoolean owner = new AtomicBoolean(true);
            View view = new View();
            UiContainerLayer layer = UiContainerLayerHost.create(backend, page(), owner::get, () -> view);
            int[] clicks = {0};
            view.onAction("refresh", () -> clicks[0]++);
            assertTrue(layer.render(null, regions()));
            if (mode == 0) {
                backend.requestSwitch(false);
                backend.tick();
            } else if (mode == 1) backend.reload();
            else owner.set(false);
            view.activate("refresh");
            assertEquals(0, clicks[0]);
            assertFalse(layer.render(null, regions()));
            assertEquals(1, view.closes);
            assertEquals(0, view.subscriptionCount());
        }
    }

    @Test
    public void malformedGeometryFallsBackBeforePainting() {
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        View view = new View();
        UiContainerLayer layer = UiContainerLayerHost.create(backend, page(), () -> true, () -> view);
        assertFalse(layer.render(null, Collections.singletonMap("panel", new ScreenCoordinate(Double.NaN, 0, 1, 1))));
        assertEquals(0, view.renders);
        assertEquals(1, view.closes);
        assertFalse(UiContainerLayerHost.valid(Collections.singletonMap("panel", new ScreenCoordinate(0, 0, Double.POSITIVE_INFINITY, 1))));
        Map<String, ScreenCoordinate> tooMany = new HashMap<>(regions());
        for (int i = 0; i < 128; i++) tooMany.put("slot_" + i, new ScreenCoordinate(0, 0, 18, 18));
        assertFalse(UiContainerLayerHost.valid(tooMany));
    }

    @Test
    public void failedPaintingSuppressesOnlyCurrentFrameThenIsolates() {
        for (boolean painted : new boolean[]{false, true}) {
            UiBackendController backend = new UiBackendController(() -> true, () -> true);
            View view = new View();
            view.failure = painted ? new AuiHudHost.PaintedFailure(new IllegalStateException("paint")) : new IllegalStateException("prepare");
            UiContainerLayer layer = UiContainerLayerHost.create(backend, page(), () -> true, () -> view);
            assertEquals(painted, layer.render(null, regions()));
            assertFalse(layer.render(null, regions()));
            assertEquals(1, view.renders);
            assertFalse(backend.wantsAui(page().id()));
            assertTrue(backend.wantsAui("test:other"));
        }
    }

    @Test
    public void lostOwnerExceptionFallsBackWithoutEscapingIntoTheContainer() {
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        AtomicBoolean fail = new AtomicBoolean(false);
        View view = new View();
        UiContainerLayer layer = UiContainerLayerHost.create(backend, page(), () -> {
            if (fail.get()) throw new IllegalStateException("Owner expired");
            return true;
        }, () -> view);
        fail.set(true);
        try {
            assertFalse(layer.render(null, regions()));
        } catch (IllegalStateException escaped) {
            fail("Owner failure escaped into the container");
        }
        assertEquals(1, view.closes);
    }

    @Test
    public void failedThemeBindingReleasesTheDocumentBeforeFallback() {
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        View view = new View() {
            @Override
            public void theme(xin.vanilla.banira.client.data.BaniraColorConfig theme) {
                throw new IllegalStateException("Theme binding failed");
            }
        };
        assertNull(UiContainerLayerHost.create(backend, page(), () -> true, () -> view));
        assertEquals(1, view.closes);
    }

    @Test
    public void explicitFallbackAndCleanupFailureCannotKeepOwnership() {
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        View view = new View() {
            @Override
            public void close() {
                super.close();
                throw new IllegalStateException("Engine release failed");
            }
        };
        UiContainerLayer layer = UiContainerLayerHost.create(backend, page(), () -> true, () -> view);
        assertTrue(layer.render(null, regions()));
        layer.fallback();
        layer.close();
        assertFalse(backend.usesAui(page().id()));
        assertFalse(backend.wantsAui(page().id()));
        assertEquals(1, view.closes);
    }
}
