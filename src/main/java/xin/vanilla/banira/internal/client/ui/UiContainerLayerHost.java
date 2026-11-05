package xin.vanilla.banira.internal.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.apache.logging.log4j.LogManager;
import xin.vanilla.banira.api.client.ui.UiContainerLayer;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class UiContainerLayerHost implements UiContainerLayer {
    public interface Renderer {
        void render(GuiGraphicsExtractor graphics, Map<String, ScreenCoordinate> regions);
    }

    private final UiBackendController backend;
    private final UiPageSpec page;
    private final BooleanSupplier current;
    private final long revision;
    private final NativeUiView view;
    private boolean closed;

    private UiContainerLayerHost(UiBackendController backend, UiPageSpec page,
                                 BooleanSupplier current, Supplier<NativeUiView> factory) {
        this.backend = backend;
        this.page = page;
        this.current = current;
        revision = backend.revision();
        view = Objects.requireNonNull(factory.get());
        view.gate(this::current);
        try {
            if (!(view instanceof Renderer)) throw new IllegalStateException("Container renderer unavailable");
            view.theme(page.theme());
        } catch (RuntimeException | LinkageError failure) {
            release(view);
            throw failure;
        }
    }

    public static UiContainerLayer create(UiBackendController backend, UiPageSpec page,
                                          BooleanSupplier current, Supplier<NativeUiView> factory) {
        try {
            if (!current.getAsBoolean() || !backend.wantsAui(page.id())) return null;
            return new UiContainerLayerHost(backend, page, current, factory);
        } catch (RuntimeException | LinkageError failure) {
            backend.isolate(page.id());
            LogManager.getLogger().warn("AUI container unavailable; preserving native appearance", failure);
            return null;
        }
    }

    private boolean current() {
        try {
            return !closed && backend.revision() == revision && backend.wantsAui(page.id()) && current.getAsBoolean();
        } catch (RuntimeException | LinkageError failure) {
            backend.isolate(page.id());
            LogManager.getLogger().warn("AUI container owner expired; preserving native inventory", failure);
            return false;
        }
    }

    @Override
    public UiView view() {
        return view;
    }

    public static boolean valid(Map<String, ScreenCoordinate> regions) {
        if (regions == null || regions.isEmpty() || regions.size() > 128 || !regions.containsKey("panel")) return false;
        for (var entry : regions.entrySet()) {
            ScreenCoordinate b = entry.getValue();
            if (entry.getKey() == null || !entry.getKey().matches("[a-z][a-z0-9_]{0,47}") || b == null
                    || !Double.isFinite(b.x()) || !Double.isFinite(b.y()) || !Double.isFinite(b.width()) || !Double.isFinite(b.height())
                    || Math.abs(b.x()) > 16384 || Math.abs(b.y()) > 16384 || b.width() <= 0 || b.height() <= 0
                    || b.width() > 16384 || b.height() > 16384) return false;
        }
        return true;
    }

    @Override
    public boolean render(GuiGraphicsExtractor graphics, Map<String, ScreenCoordinate> regions) {
        if (!current() || !valid(regions)) {
            close();
            return false;
        }
        try {
            view.theme(page.theme());
            ((Renderer) view).render(graphics, regions);
            backend.attach(page.id());
            return true;
        } catch (RuntimeException | LinkageError failure) {
            boolean painted = failure instanceof AuiHudHost.PaintedFailure;
            backend.isolate(page.id());
            close();
            LogManager.getLogger().warn("AUI container failed; preserving native inventory", failure);
            return painted;
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            release(view);
        } finally {
            backend.detach(page.id());
        }
    }

    @Override
    public void fallback() {
        backend.isolate(page.id());
        close();
    }

    private static void release(NativeUiView view) {
        try {
            view.close();
        } catch (RuntimeException | LinkageError failure) {
            LogManager.getLogger().warn("Unable to release AUI container appearance", failure);
        }
    }
}
