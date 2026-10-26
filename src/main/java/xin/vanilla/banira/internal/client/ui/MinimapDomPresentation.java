package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.ScreenCoordinate;
import java.util.Objects;

/** Unsupported compatibility shim; minimap text must never allocate an AUI DOM row. */
@Deprecated
public final class MinimapDomPresentation implements AutoCloseable {
    private static final class Row { }
    public MinimapDomPresentation(UiDomAccess dom) { Objects.requireNonNull(dom); }

    public void select(String key, String text, int color, boolean shadow) {
        throw new UnsupportedOperationException("Minimap rows use native rendering");
    }

    public ScreenCoordinate measure() { return null; }

    @Override public void close() {
    }
}
