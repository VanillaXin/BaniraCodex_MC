package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class QuickActionHudPresentation implements AutoCloseable {
    private final UiDomAccess dom;
    private final Function<ScreenCoordinate, ScreenCoordinate> projection;
    private final List<UiDomAccess.Node> masks = new ArrayList<>(), nodes = new ArrayList<>();
    private final List<String> styles = new ArrayList<>(), clips = new ArrayList<>(), labels = new ArrayList<>();
    private boolean closed;

    public QuickActionHudPresentation(UiDomAccess dom, Function<ScreenCoordinate, ScreenCoordinate> projection) {
        this.dom = dom;
        this.projection = projection;
    }

    public void update(QuickActionHudFrame frame) {
        if (closed || !dom.current()) return;
        int count = frame == null ? 0 : frame.elements().size();
        for (int i = 0; i < count; i++) {
            lombok.var part = frame.elements().get(i);
            if (i == nodes.size()) {
                lombok.var mask = dom.create("div");
                lombok.var node = dom.create("div");
                node.attribute("class", "quick-hud-element");
                mask.append(node);
                dom.find("overlay").append(mask);
                masks.add(mask);
                nodes.add(node);
                styles.add("");
                clips.add("");
                labels.add("");
            }
            ScreenCoordinate b = projection.apply(part.bounds());
            double units = part.bounds().height() > 0 ? b.height() / part.bounds().height() : 1;
            String clip = "position:absolute;pointer-events:none;z-index:" + (int) part.depth() + ";";
            if (part.clip() == null) clip += "left:0;top:0;width:100%;height:100%;";
            else {
                ScreenCoordinate c = projection.apply(part.clip());
                clip += position(c) + "overflow:hidden;";
                b = new ScreenCoordinate(b.x() - c.x(), b.y() - c.y(), b.width(), b.height());
            }
            String style = position(b) + "box-sizing:border-box;pointer-events:none;overflow:hidden;";
            String accent = HudDrawList.css(part.accent());
            switch (part.kind()) {
                case SLOT:
                    style += "background:" + (part.empty() ? "transparent" : HudDrawList.css(part.background()))
                            + ";border:" + units + "px " + (part.empty() ? "dashed " : "solid ")
                            + (part.active() ? accent : HudDrawList.css(part.border())) + ";border-radius:" + 2 * units + "px;";
                    break;
                case MENU:
                    style += "background:" + HudDrawList.css(part.background()) + ";border:" + units
                            + "px solid " + HudDrawList.css(part.border()) + ";border-radius:" + 3 * units + "px;";
                    break;
                case ROW:
                    style += "background:" + (part.active() ? HudDrawList.css((part.accent() & 0xffffff) | 0x22000000) : "transparent")
                            + ";border-radius:" + 2 * units + "px;";
                    break;
                case SCROLL:
                    style += "background:" + accent + ";border-radius:" + units + "px;";
                    break;
                case TITLE:
                    style += "font-weight:600;";
                    break;
            }
            style += "font-family:default;font-size:" + 7 * units + "px;line-height:" + b.height()
                    + "px;white-space:nowrap;color:" + HudDrawList.css(part.foreground()) + ";";
            if (!style.equals(styles.get(i))) {
                nodes.get(i).attribute("style", style);
                styles.set(i, style);
            }
            if (!clip.equals(clips.get(i))) {
                masks.get(i).attribute("style", clip);
                clips.set(i, clip);
            }
            if (!part.text().equals(labels.get(i))) {
                nodes.get(i).text(part.text());
                labels.set(i, part.text());
            }
        }
        while (nodes.size() > count) removeLast();
    }

    private static String position(ScreenCoordinate b) {
        return "position:absolute;left:" + b.x() + "px;top:" + b.y() + "px;width:" + b.width() + "px;height:" + b.height() + "px;";
    }

    private void removeLast() {
        int i = nodes.size() - 1;
        nodes.remove(i).remove();
        masks.remove(i).remove();
        styles.remove(i);
        clips.remove(i);
        labels.remove(i);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        while (!nodes.isEmpty()) removeLast();
    }
}
