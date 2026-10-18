package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.ScreenCoordinate;
import java.util.*;

public final class MinimapDomPresentation implements AutoCloseable {
    private final UiDomAccess dom;
    private final Map<String, Row> rows = new LinkedHashMap<>();
    private Row selected;
    private boolean closed;

    public MinimapDomPresentation(UiDomAccess dom) { this.dom = Objects.requireNonNull(dom); }

    public void select(String key, String text, int color, boolean shadow) {
        if (closed || !dom.current()) throw new IllegalStateException("Minimap UI detached");
        Row row = rows.get(key);
        if (row == null) {
            if (rows.size() >= 120) throw new IllegalStateException("Minimap UI row budget");
            row = new Row(dom.create("div"));
            dom.find("rows").append(row.node); rows.put(key, row);
        }
        if (selected != row && selected != null) selected.style(false);
        if (!text.equals(row.text)) { row.node.text(text); row.text = text; }
        row.color = color; row.shadow = shadow; row.style(true); selected = row;
    }

    public ScreenCoordinate measure() { return selected == null ? null : selected.node.bounds(); }

    @Override public void close() {
        if (closed) return;
        closed = true; for (Row row : rows.values()) row.node.remove(); rows.clear(); selected = null;
    }

    private static final class Row {
        final UiDomAccess.Node node;
        String text = "", applied = "";
        int color;
        boolean shadow;
        Row(UiDomAccess.Node node) { this.node = node; }
        void style(boolean visible) {
            int argb = (color & 0xff000000) == 0 ? color | 0xff000000 : color;
            String next = "position:absolute;left:0;top:0;width:max-content;height:auto;margin:0;padding:0;border:0;"
                    + "white-space:pre;font-size:9px;line-height:9px;pointer-events:none;visibility:"
                    + (visible ? "visible" : "hidden") + ";color:" + HudDrawList.css(argb)
                    + ";text-shadow:" + (shadow ? "1px 1px #00000080" : "none") + ";";
            if (!next.equals(applied)) { node.attribute("style", next); applied = next; }
        }
    }
}
