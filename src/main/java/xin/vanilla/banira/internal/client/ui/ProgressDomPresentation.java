package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.api.client.ui.UiHudFrame;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import java.util.*;
import java.util.function.Function;

public final class ProgressDomPresentation implements AutoCloseable {
    private final UiDomAccess dom;
    private final Function<ScreenCoordinate, ScreenCoordinate> projection;
    private final Map<String, PartNodes> nodes = new LinkedHashMap<>();
    private boolean closed;
    private List<String> order = List.of();

    public ProgressDomPresentation(UiDomAccess dom, Function<ScreenCoordinate, ScreenCoordinate> projection) {
        this.dom = Objects.requireNonNull(dom); this.projection = Objects.requireNonNull(projection);
        if (dom.find("progress") == null) throw new IllegalStateException("Missing progress root");
    }

    public void update(UiHudFrame frame) {
        if (closed || !dom.current()) return;
        Set<String> keep = new HashSet<>();
        UiDomAccess.Node root = dom.find("progress");
        for (UiHudFrame.Part part : frame.parts()) {
            keep.add(part.id());
            boolean sprite = part instanceof UiHudFrame.Sprite;
            PartNodes entry = nodes.get(part.id());
            if (entry != null && entry.sprite != sprite) { entry.close(); nodes.remove(part.id()); entry = null; }
            if (entry == null) {
                entry = new PartNodes(dom.create(sprite ? "texture" : "div"), sprite);
                nodes.put(part.id(), entry); root.append(entry.root);
                if (!sprite) {
                    entry.shadow = dom.create("div"); entry.text = dom.create("div");
                    entry.root.append(entry.shadow); entry.root.append(entry.text);
                }
            }
            if (sprite) {
                UiHudFrame.Sprite s = (UiHudFrame.Sprite) part;
                ScreenCoordinate b = project(s.x(), s.y(), s.width(), s.height());
                entry.attribute("src", s.texture().toString());
                entry.style(position(b) + "transform-origin:50% 50%;transform:rotate(" + s.angleDegrees() + "deg);");
            } else {
                UiHudFrame.Text t = (UiHudFrame.Text) part;
                ScreenCoordinate b = project(t.x(), t.y(), t.width(), t.height());
                ScreenCoordinate pivot = project(0, 0, t.pivotX(), t.pivotY());
                ScreenCoordinate unit = project(0, 0, 1, t.fontSize());
                entry.style(position(b) + "transform-origin:0 0;transform:rotate(" + Math.toDegrees(t.angleRadians())
                        + "deg) scale(" + t.scale() + ") translate(" + -pivot.width() + "px," + -pivot.height() + "px);");
                String base = "position:absolute;margin:0;padding:0;border:0;white-space:pre;pointer-events:none;"
                        + "width:" + b.width() + "px;height:" + b.height() + "px;font-size:" + unit.height() + "px;line-height:" + unit.height() + "px;";
                int color = (t.color() & 0xff000000) == 0 ? t.color() | 0xff000000 : t.color();
                String foreground = base + "left:0;top:0;color:" + HudDrawList.css(color) + ";";
                String shadow = base + "left:" + unit.width() + "px;top:" + unit.width() + "px;color:"
                        + HudDrawList.css((color & 0xfcfcfc) >> 2 | color & 0xff000000) + ";";
                if (!foreground.equals(entry.foreground)) { entry.text.attribute("style", foreground); entry.foreground = foreground; }
                if (!shadow.equals(entry.shadowStyle)) { entry.shadow.attribute("style", shadow); entry.shadowStyle = shadow; }
                if (!t.value().equals(entry.value)) { entry.text.text(t.value()); entry.shadow.text(t.value()); entry.value = t.value(); }
            }
        }
        var iterator = nodes.entrySet().iterator();
        while (iterator.hasNext()) { var entry = iterator.next(); if (!keep.contains(entry.getKey())) { entry.getValue().close(); iterator.remove(); } }
        List<String> nextOrder = frame.parts().stream().map(UiHudFrame.Part::id).toList();
        if (!order.equals(nextOrder)) {
            for (String id : nextOrder) root.append(nodes.get(id).root);
            order = nextOrder;
        }
    }

    private ScreenCoordinate project(double x, double y, double width, double height) {
        ScreenCoordinate b = projection.apply(new ScreenCoordinate(x, y, width, height));
        if (b == null || !Double.isFinite(b.x()) || !Double.isFinite(b.y()) || !Double.isFinite(b.width()) || !Double.isFinite(b.height()))
            throw new IllegalStateException("HUD projection unavailable");
        return b;
    }
    private static String position(ScreenCoordinate b) {
        return "position:absolute;margin:0;padding:0;border:0;background:transparent;pointer-events:none;left:" + b.x() + "px;top:"
                + b.y() + "px;width:" + b.width() + "px;height:" + b.height() + "px;";
    }
    @Override public void close() { if (closed) return; closed = true; nodes.values().forEach(PartNodes::close); nodes.clear(); order = List.of(); }

    private static final class PartNodes {
        final UiDomAccess.Node root;
        final boolean sprite;
        UiDomAccess.Node text, shadow;
        String style, source, foreground, shadowStyle, value;
        PartNodes(UiDomAccess.Node root, boolean sprite) { this.root = root; this.sprite = sprite; }
        void attribute(String key, String next) { if (!next.equals(source)) { root.attribute(key, next); source = next; } }
        void style(String next) { if (!next.equals(style)) { root.attribute("style", next); style = next; } }
        void close() { if (text != null) text.remove(); if (shadow != null) shadow.remove(); root.remove(); }
    }
}
