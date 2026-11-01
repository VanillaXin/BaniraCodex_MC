package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class UiCorePortTest {
    private static UiPageSpec page(UiPageSession session) {
        return new UiPageSpec("banira_codex:test", "banira_codex/ui/form.html", session, previous -> new Object());
    }

    private static final class Session implements UiPageSession {
        int closes;
        public void bind(UiView view) { }
        public void detach(UiDetachReason reason) { }
        public void close(UiCloseReason reason) { closes++; }
    }

    @Test
    public void unavailableAuiNeverAllocatesContainerAppearance() {
        AtomicInteger allocations = new AtomicInteger();
        assertNull(UiContainerLayerHost.create(new UiBackendController(() -> true, () -> false),
                page(new Session()), () -> true, () -> {
                    allocations.incrementAndGet();
                    return new NativeUiView(new Object());
                }));
        assertEquals(0, allocations.get());
    }

    @Test
    public void invalidGeometryReleasesAppearanceWithoutPaintingOrClaimingOwnership() {
        class View extends NativeUiView implements UiContainerLayerHost.Renderer {
            int paints;
            View() { super(new Object()); }
            public void render(GuiGraphics graphics, Map<String, ScreenCoordinate> regions) { paints++; }
        }
        UiBackendController backend = new UiBackendController(() -> true, () -> true);
        View view = new View();
        UiPageSpec page = page(new Session());
        UiContainerLayer layer = UiContainerLayerHost.create(backend, page, () -> true, () -> view);
        assertNotNull(layer);
        assertFalse(layer.render(null, Map.of("panel", new ScreenCoordinate(Double.NaN, 0, 100, 100))));
        assertEquals(0, view.paints);
        assertTrue(view.closed());
        assertFalse(backend.usesAui(page.id()));
    }

    @Test
    public void hudFrameRejectsDuplicateIdsAndNonFiniteGeometry() {
        UiHudFrame.Text text = new UiHudFrame.Text("clock", "00:10", 0, 0, 40, 10, 9, 1, 0, 0, 0, -1);
        assertEquals(1, new UiHudFrame(List.of(text)).parts().size());
        assertThrows(IllegalArgumentException.class, () -> new UiHudFrame(List.of(text, text)));
        assertThrows(IllegalArgumentException.class, () -> new UiHudFrame.Text("clock", "00:10", Double.NaN, 0, 40, 10, 9, 1, 0, 0, 0, -1));
    }

    @Test
    public void nativeSessionDisconnectIsIdempotentAndDoesNotCloseTransferredSessions() {
        class Source implements UiPageSource {
            final Session session = new Session();
            public String uiSurfaceId() { return "banira_codex:test"; }
            public UiPageSpec uiPage() { return page(session); }
            public UiPageSession pageSession() { return session; }
        }
        UiNativeSessions sessions = new UiNativeSessions();
        Source retained = new Source(), transferred = new Source();
        sessions.remember(retained);
        sessions.remember(retained);
        sessions.remember(transferred);
        sessions.forget(transferred, true);
        sessions.disconnect();
        sessions.disconnect();
        assertEquals(1, retained.session.closes);
        assertEquals(0, transferred.session.closes);
    }
}
