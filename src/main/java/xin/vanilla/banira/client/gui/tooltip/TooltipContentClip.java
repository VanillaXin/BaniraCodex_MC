package xin.vanilla.banira.client.gui.tooltip;

/**
 * 根据当前动画边界与有效内边距计算 Tooltip 文字内容裁剪区。
 */
public final class TooltipContentClip {
    private TooltipContentClip() {
    }

    public static TooltipBounds inset(TooltipBounds bounds,
                                      int paddingLeft, int paddingRight,
                                      int paddingTop, int paddingBottom) {
        double left = Math.min(bounds.width(), Math.max(0, paddingLeft));
        double top = Math.min(bounds.height(), Math.max(0, paddingTop));
        double width = Math.max(0.0D, bounds.width() - left - Math.max(0, paddingRight));
        double height = Math.max(0.0D, bounds.height() - top - Math.max(0, paddingBottom));
        return new TooltipBounds(bounds.x() + left, bounds.y() + top, width, height);
    }
}
