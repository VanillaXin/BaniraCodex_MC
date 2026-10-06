package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.common.enums.EnumPosition;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Predicate;

/**
 * Per-frame placement of notification rectangles, independent of rendering and timing.
 */
public final class NotificationRegionLayout {
    public static final class Rect {
        public final int x, y, width, height;

        public Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean overlaps(Rect other) {
            return x < other.x + other.width && x + width > other.x
                    && y < other.y + other.height && y + height > other.y;
        }

        public boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    private final int maximum;
    private final List<Rect> placed = new ArrayList<>();
    private final List<Rect> moving = new ArrayList<>();
    private final EnumMap<EnumPosition, Integer> counts = new EnumMap<>(EnumPosition.class);
    private final EnumMap<EnumPosition, Integer> usedHeight = new EnumMap<>(EnumPosition.class);

    public NotificationRegionLayout(int maximum) {
        this.maximum = Math.max(0, maximum);
    }

    public static <T> List<T> admissionOrder(List<T> entries, Predicate<T> active) {
        List<T> result = new ArrayList<>(entries.size());
        for (T entry : entries) if (active.test(entry)) result.add(entry);
        for (T entry : entries) if (!active.test(entry)) result.add(entry);
        return result;
    }

    public void follow(EnumPosition position, Rect area, Rect current, int gap) {
        int occupied = position.isBottom() ? area.y + area.height - current.y : current.y + current.height - area.y;
        usedHeight.put(position, Math.max(usedHeight.getOrDefault(position, 0), occupied + Math.max(0, gap)));
        moving.add(current);
    }

    public static Rect region(EnumPosition position, int sw, int sh, int widthPercent, int heightPercent, int margin) {
        int pad = Math.max(0, Math.min(margin, Math.min(sw, sh) / 4));
        int w = Math.max(1, Math.min(sw - pad * 2, Math.max(48, sw * widthPercent / 100)));
        int h = Math.max(1, Math.min(sh - pad * 2, Math.max(32, sh * heightPercent / 100)));
        return new Rect(horizontal(position, pad, sw - pad * 2, w),
                position.isTop() ? pad : position.isBottom() ? sh - pad - h : (sh - h) / 2, w, h);
    }

    private static int horizontal(EnumPosition pos, int x, int areaWidth, int width) {
        switch (pos) {
            case TOP_LEFT:
            case LEFT_CENTER:
            case BOTTOM_LEFT:
                return x;
            case TOP_RIGHT:
            case RIGHT_CENTER:
            case BOTTOM_RIGHT:
                return x + areaWidth - width;
            default:
                return x + (areaWidth - width) / 2;
        }
    }

    public Rect place(EnumPosition position, Rect area, int width, int height, int gap, int limit) {
        int count = counts.getOrDefault(position, 0);
        int used = usedHeight.getOrDefault(position, 0);
        if (placed.size() >= maximum || count >= limit || width > area.width || height > area.height - used)
            return null;
        Rect rect = new Rect(horizontal(position, area.x, area.width, width),
                position.isBottom() ? area.y + area.height - used - height : area.y + used, width, height);
        for (Rect previous : placed) if (rect.overlaps(previous)) return null;
        for (Rect previous : moving) if (rect.overlaps(previous)) return null;
        counts.put(position, count + 1);
        usedHeight.put(position, used + height + Math.max(0, gap));
        placed.add(rect);
        return rect;
    }
}
