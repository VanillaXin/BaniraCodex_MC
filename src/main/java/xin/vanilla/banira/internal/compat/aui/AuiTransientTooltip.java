package xin.vanilla.banira.internal.compat.aui;

import com.sighs.apricityui.init.Document;
import com.sighs.apricityui.init.Element;
import com.sighs.apricityui.layout.Position;
import xin.vanilla.banira.client.data.ScreenCoordinate;

final class AuiTransientTooltip implements AutoCloseable {
    private final Element root;
    private Element owner;
    private String text = "", style = "";
    private long entered;

    AuiTransientTooltip(Document document, Element body) {
        root = Element.init(document.createElement("div"));
        root.setAttribute("class", "banira-tooltip");
        root.setAttribute("role", "tooltip");
        root.setTopLayer(true);
        hide();
        body.appendChild(root);
    }

    boolean owns(Element element) {
        return owner == element;
    }

    ScreenCoordinate visibleBounds(Document document) {
        if (owner == null || !root.isVisible || System.nanoTime() - entered < 350_000_000L) return null;
        Element.DOMRect rect = root.getBoundingClientRect();
        Position start = document.documentToGuiPosition(new Position(rect.x, rect.y));
        Position end = document.documentToGuiPosition(new Position(rect.right, rect.bottom));
        return end.x > start.x && end.y > start.y ? new ScreenCoordinate(start.x, start.y, end.x - start.x, end.y - start.y) : null;
    }

    void update(Element next, String value, Position point, double width, double height) {
        if (next == null || value == null || AuiTransientLayout.blank(value)) {
            hide();
            return;
        }
        if (owner != next || !text.equals(value)) {
            hide();
            owner = next;
            text = value;
            entered = System.nanoTime();
            root.setInnerText(value);
        }
        if (System.nanoTime() - entered < 350_000_000L) return;
        ScreenCoordinate bounds = AuiTransientLayout.tooltip(value, point.x, point.y, width, height);
        css("position:fixed;pointer-events:none;z-index:10001;display:block;left:" + bounds.x() + "px;top:" + bounds.y()
                + "px;width:" + bounds.width() + "px;max-height:" + Math.max(0, height - 8) + "px");
    }

    void hide() {
        owner = null;
        css("position:fixed;pointer-events:none;z-index:10001;display:none");
    }

    private void css(String value) {
        if (!style.equals(value)) {
            style = value;
            root.setAttribute("style", value);
        }
    }

    @Override
    public void close() {
        hide();
        root.remove();
    }
}
