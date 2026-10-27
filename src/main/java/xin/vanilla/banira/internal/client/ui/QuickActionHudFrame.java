package xin.vanilla.banira.internal.client.ui;

import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Visual metadata only. The inventory overlay remains the input and layout owner. */
public final class QuickActionHudFrame {
    public enum Kind { SLOT, MENU, ROW, TITLE, SCROLL }
    public record Element(Kind kind, ScreenCoordinate bounds, ScreenCoordinate clip, String text,
                          int background, int border, int foreground, int accent, boolean active,
                          boolean empty, double depth) { }
    private final List<Element> elements = new ArrayList<>();

    public List<Element> elements() { return Collections.unmodifiableList(elements); }

    public boolean occludes(ScreenCoordinate icon, double depth) {
        return elements.stream().anyMatch(e -> e.kind() == Kind.MENU && e.depth() > depth
                && icon.x() < e.bounds().x() + e.bounds().width() && icon.x() + icon.width() > e.bounds().x()
                && icon.y() < e.bounds().y() + e.bounds().height() && icon.y() + icon.height() > e.bounds().y());
    }

    /** Native icons paint after the DOM; exclude opaque menus above their original depth. */
    public List<ScreenCoordinate> uncovered(ScreenCoordinate clip, double depth) {
        List<ScreenCoordinate> regions = List.of(clip);
        for (Element element : elements) {
            if (element.kind() != Kind.MENU || element.depth() <= depth) continue;
            List<ScreenCoordinate> next = new ArrayList<>();
            for (ScreenCoordinate region : regions) {
                ScreenCoordinate menu = element.bounds();
                double left = Math.max(region.x(), menu.x()), top = Math.max(region.y(), menu.y());
                double right = Math.min(region.x() + region.width(), menu.x() + menu.width());
                double bottom = Math.min(region.y() + region.height(), menu.y() + menu.height());
                if (left >= right || top >= bottom) { next.add(region); continue; }
                if (top > region.y()) next.add(new ScreenCoordinate(region.x(), region.y(), region.width(), top - region.y()));
                if (bottom < region.y() + region.height()) next.add(new ScreenCoordinate(region.x(), bottom, region.width(), region.y() + region.height() - bottom));
                if (left > region.x()) next.add(new ScreenCoordinate(region.x(), top, left - region.x(), bottom - top));
                if (right < region.x() + region.width()) next.add(new ScreenCoordinate(right, top, region.x() + region.width() - right, bottom - top));
            }
            regions = next;
        }
        return List.copyOf(regions);
    }

    public void add(Kind kind, double x, double y, double width, double height, ScreenCoordinate clip,
                    String text, BaniraColorConfig theme, boolean active, boolean empty, double depth) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width)
                || !Double.isFinite(height) || !Double.isFinite(depth) || width < 0 || height < 0)
            throw new IllegalArgumentException("Quick action geometry");
        if (elements.size() >= 512) throw new IllegalStateException("Quick action node budget");
        elements.add(new Element(kind, new ScreenCoordinate(x, y, width, height),
                clip == null ? null : new ScreenCoordinate(clip.x(), clip.y(), clip.width(), clip.height()),
                text == null ? "" : text, theme.bgSurface() | 0xff000000, theme.border() | 0xff000000,
                theme.textPrimary() | 0xff000000, theme.accentHover() | 0xff000000, active, empty, depth));
    }
}
