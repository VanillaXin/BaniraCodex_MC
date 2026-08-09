package xin.vanilla.banira.client.gui.quickaction;

import lombok.Value;
import lombok.experimental.Accessors;

/** GUI 坐标系中的不可变矩形，用于快捷入口布局与命中判断。 */
@Value
@Accessors(fluent = true)
public class QuickActionRect {
    int x;
    int y;
    int width;
    int height;

    public int right() {
        return x + Math.max(0, width);
    }

    public int bottom() {
        return y + Math.max(0, height);
    }

    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    /** margin 同时作为两个矩形之间需要保留的最小空隙。 */
    public boolean intersects(QuickActionRect other, int margin) {
        if (other == null || isEmpty() || other.isEmpty()) {
            return false;
        }
        int gap = Math.max(0, margin);
        return right() > other.x - gap
                && x < other.right() + gap
                && bottom() > other.y - gap
                && y < other.bottom() + gap;
    }

    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < right() && py < bottom();
    }
}
