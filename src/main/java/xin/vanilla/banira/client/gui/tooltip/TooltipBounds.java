package xin.vanilla.banira.client.gui.tooltip;

import java.util.Objects;

/** Tooltip 在 GUI 坐标系中的浮点边界。 */
public final class TooltipBounds {
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public TooltipBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0.0D, width);
        this.height = Math.max(0.0D, height);
    }

    public TooltipBounds interpolate(TooltipBounds target, double progress) {
        double t = Math.max(0.0D, Math.min(1.0D, progress));
        return new TooltipBounds(
                x + (target.x - x) * t,
                y + (target.y - y) * t,
                width + (target.width - width) * t,
                height + (target.height - height) * t
        );
    }

    public TooltipBounds translate(double dx, double dy) {
        return new TooltipBounds(x + dx, y + dy, width, height);
    }

    public TooltipBounds collapseToCenter() {
        return new TooltipBounds(x + width / 2.0D, y + height / 2.0D, 0.0D, 0.0D);
    }

    /** 保持完整宽度，并向最接近鼠标的水平边缘收缩。 */
    public TooltipBounds collapseToVerticalEdge(double pointerY) {
        if (Double.isNaN(pointerY)) {
            return collapseToCenter();
        }
        double centerY = y + height / 2.0D;
        double edgeY = pointerY <= centerY ? y : y + height;
        return new TooltipBounds(x, edgeY, width, 0.0D);
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TooltipBounds)) return false;
        TooltipBounds other = (TooltipBounds) obj;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(width, other.width) == 0
                && Double.compare(height, other.height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }
}
