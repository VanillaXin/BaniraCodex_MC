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
    private List<String> order = com.google.common.collect.ImmutableList.of();

    public ProgressDomPresentation(UiDomAccess dom, Function<ScreenCoordinate, ScreenCoordinate> projection) {
        this.dom = Objects.requireNonNull(dom);
        this.projection = Objects.requireNonNull(projection);
        if (dom.find("progress") == null) throw new IllegalStateException("Missing progress root");
    }

    public void update(UiHudFrame frame) {
        if (closed || !dom.current()) return;
        Set<String> keep = new HashSet<>();
        boolean rebuildOrder = false;
        UiDomAccess.Node root = dom.find("progress");
        for (UiHudFrame.Part part : frame.parts()) {
            keep.add(part.id());
            boolean sprite = part instanceof UiHudFrame.Sprite;
            PartNodes entry = nodes.get(part.id());
            if (entry != null && entry.sprite != sprite) {
                entry.close();
                nodes.remove(part.id());
                entry = null;
                rebuildOrder = true;
            }
            if (entry == null) {
                entry = new PartNodes(dom.create(sprite ? "texture" : "div"), sprite);
                nodes.put(part.id(), entry);
                root.append(entry.root);
                entry.root.hudId(part.id());
                entry.root.attribute("class", sprite ? "progress-sprite" : "progress-text");
                if (!sprite) {
                    entry.shadow = dom.create("div");
                    entry.text = dom.create("div");
                    entry.shadow.attribute("class", "progress-shadow");
                    entry.text.attribute("class", "progress-value");
                    entry.root.append(entry.shadow);
                    entry.root.append(entry.text);
                }
            }
            if (sprite) {
                UiHudFrame.Sprite s = (UiHudFrame.Sprite) part;
                ScreenCoordinate b = project(s.x(), s.y(), s.width(), s.height());
                entry.attribute("src", s.texture().toString());
                entry.position(b);
                entry.variable("angle", s.angleDegrees() + "deg");
            } else {
                UiHudFrame.Text t = (UiHudFrame.Text) part;
                ScreenCoordinate b = project(t.x(), t.y(), t.width(), t.height());
                ScreenCoordinate pivot = project(0, 0, t.pivotX(), t.pivotY());
                ScreenCoordinate unit = project(0, 0, 1, t.fontSize());
                entry.position(b);
                entry.variable("angle", Math.toDegrees(t.angleRadians()) + "deg");
                entry.variable("scale", Double.toString(t.scale()));
                entry.variable("pivot-x", -pivot.width() + "px");
                entry.variable("pivot-y", -pivot.height() + "px");
                entry.variable("font-size", unit.height() + "px");
                entry.variable("shadow-offset", unit.width() + "px");
                int color = (t.color() & 0xff000000) == 0 ? t.color() | 0xff000000 : t.color();
                entry.variable("color", HudDrawList.css(color));
                entry.variable("shadow-color", HudDrawList.css((color & 0xfcfcfc) >> 2 | color & 0xff000000));
                if (!t.value().equals(entry.value)) {
                    entry.text.text(t.value());
                    entry.shadow.text(t.value());
                    entry.value = t.value();
                }
            }
        }
        lombok.var iterator = nodes.entrySet().iterator();
        while (iterator.hasNext()) {
            lombok.var entry = iterator.next();
            if (!keep.contains(entry.getKey())) {
                entry.getValue().close();
                iterator.remove();
            }
        }
        List<String> nextOrder = frame.parts().stream().map(UiHudFrame.Part::id).collect(java.util.stream.Collectors.toList());
        if (rebuildOrder || !order.equals(nextOrder)) {
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

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        nodes.values().forEach(PartNodes::close);
        nodes.clear();
        order = com.google.common.collect.ImmutableList.of();
    }

    private static final class PartNodes {
        final UiDomAccess.Node root;
        final boolean sprite;
        UiDomAccess.Node text, shadow;
        String source, value;
        final Map<String, String> variables = new HashMap<>();

        PartNodes(UiDomAccess.Node root, boolean sprite) {
            this.root = root;
            this.sprite = sprite;
        }

        void attribute(String key, String next) {
            if (!next.equals(source)) {
                root.attribute(key, next);
                source = next;
            }
        }

        void position(ScreenCoordinate b) {
            variable("x", b.x() + "px");
            variable("y", b.y() + "px");
            variable("width", b.width() + "px");
            variable("height", b.height() + "px");
        }

        void variable(String name, String value) {
            if (!value.equals(variables.get(name))) {
                root.styleProperty("--hud-" + name, value);
                variables.put(name, value);
            }
        }

        void close() {
            if (text != null) text.remove();
            if (shadow != null) shadow.remove();
            root.remove();
        }
    }
}
