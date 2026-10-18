package xin.vanilla.banira.internal.client.ui;

import java.util.*;
import xin.vanilla.banira.client.data.ScreenCoordinate;

public final class HudDomPresentation implements AutoCloseable {
    private final UiDomAccess dom;
    private final List<UiDomAccess.Node> nodes = new ArrayList<>();
    private final List<UiDomAccess.Node> masks = new ArrayList<>();
    private final List<String> styles = new ArrayList<>();
    private final List<String> maskStyles = new ArrayList<>();
    private boolean closed;
    private final java.util.function.Function<ScreenCoordinate, ScreenCoordinate> projection;
    public HudDomPresentation(UiDomAccess dom) { this(dom, java.util.function.Function.identity()); }
    public HudDomPresentation(UiDomAccess dom, java.util.function.Function<ScreenCoordinate, ScreenCoordinate> projection) {
        this.dom = Objects.requireNonNull(dom); this.projection = Objects.requireNonNull(projection);
    }
    public void update(HudDrawList frame) {
        if (closed || !dom.current()) return;
        if (frame.parts().size() > 512) throw new IllegalStateException("AUI HUD node budget");
        int index = 0;
        for (HudDrawList.Part part : frame.parts()) {
            if (part.kind != HudDrawList.Kind.BOX && part.kind != HudDrawList.Kind.NOTIFICATION) continue;
            ScreenCoordinate bounds = projection.apply(new ScreenCoordinate(part.x, part.y, part.width, part.height));
            ScreenCoordinate clip = part.clip == null ? null : projection.apply(part.clip);
            String maskStyle = "position:absolute; pointer-events:none; z-index:" + (int) part.depth + ";";
            if (clip == null) maskStyle += "left:0;top:0;width:100%;height:100%;";
            else {
                maskStyle += "left:" + clip.x() + "px;top:" + clip.y() + "px;width:" + clip.width() + "px;height:" + clip.height() + "px;overflow:hidden;";
                bounds = new ScreenCoordinate(bounds.x() - clip.x(), bounds.y() - clip.y(), bounds.width(), bounds.height());
            }
            String style = part.style(bounds);
            if (part.kind == HudDrawList.Kind.NOTIFICATION) {
                var n = part.notification;
                double scale = Math.max(.01, n.renderScale());
                int alpha = n.renderAlpha() < 0 ? 255 : Math.max(1, n.renderAlpha());
                String origin = n.renderScaleCenter() == null ? "0% 0%" : origin(n.renderScaleCenter().name());
                double units = part.width <= 0 ? 1 : bounds.width() / part.width;
                style = "position:absolute; pointer-events:none; left:" + bounds.x() + "px; top:" + bounds.y() + "px; width:" + bounds.width() + "px; height:" + bounds.height()
                        + "px; background-color:" + HudDrawList.css(n.bgColor().argb()) + "; border:" + n.borderSize() * units + "px solid " + HudDrawList.css(n.borderColor().argb())
                        + "; box-sizing:border-box; border-radius:" + n.radius() * units + "px; opacity:" + alpha / 255.0 + "; transform:scale(" + scale + "); transform-origin:" + origin + ";";
            }
            if (index == nodes.size()) {
                UiDomAccess.Node node = dom.create("div"), mask = dom.create("div");
                mask.append(node); dom.find("overlay").append(mask);
                nodes.add(node); masks.add(mask); styles.add(""); maskStyles.add("");
            }
            if (!style.equals(styles.get(index))) { nodes.get(index).attribute("style", style); styles.set(index, style); }
            if (!maskStyle.equals(maskStyles.get(index))) { masks.get(index).attribute("style", maskStyle); maskStyles.set(index, maskStyle); }
            index++;
        }
        while (nodes.size() > index) removeLast();
    }
    private static String origin(String name) {
        return (name.contains("RIGHT") ? "100%" : name.contains("LEFT") ? "0%" : "50%") + " "
                + (name.startsWith("BOTTOM") ? "100%" : name.startsWith("TOP") ? "0%" : "50%");
    }
    private void removeLast() {
        nodes.remove(nodes.size() - 1).remove(); masks.remove(masks.size() - 1).remove();
        styles.remove(styles.size() - 1); maskStyles.remove(maskStyles.size() - 1);
    }
    @Override public void close() { if (closed) return; closed = true; while (!nodes.isEmpty()) removeLast(); }
}
