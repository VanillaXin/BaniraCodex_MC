package xin.vanilla.banira.client.gui.tooltip;

/** 计算水平居中于鼠标、优先显示在鼠标上方的 Tooltip 位置。 */
public final class TooltipPlacement {

    private static final double ABOVE_GAP = 5.0D;
    private static final double BELOW_GAP = 6.0D;

    private TooltipPlacement() {
    }

    public static TooltipBounds place(double pointerX, double pointerY,
                                      double width, double height,
                                      double screenWidth, double screenHeight,
                                      double marginLeft, double marginRight,
                                      double marginTop, double marginBottom) {
        double maxX = Math.max(marginLeft, screenWidth - marginRight - width);
        double x = clamp(pointerX - width / 2.0D, marginLeft, maxX);

        double aboveY = pointerY - height - ABOVE_GAP;
        double belowY = pointerY + BELOW_GAP;
        double maxY = Math.max(marginTop, screenHeight - marginBottom - height);
        double y;
        if (aboveY >= marginTop) {
            y = aboveY;
        } else if (belowY + height <= screenHeight - marginBottom) {
            y = belowY;
        } else {
            y = aboveY;
        }
        return new TooltipBounds(x, clamp(y, marginTop, maxY), width, height);
    }

    /** 计算与完整气泡位于鼠标同侧的一字气泡锚点。 */
    public static TooltipBounds anchor(TooltipBounds target, double pointerX, double pointerY,
                                       double width, double height,
                                       double screenWidth, double screenHeight,
                                       double marginLeft, double marginRight,
                                       double marginTop, double marginBottom) {
        double availableWidth = Math.max(0.0D, screenWidth - marginLeft - marginRight);
        double availableHeight = Math.max(0.0D, screenHeight - marginTop - marginBottom);
        double anchorWidth = Math.min(Math.max(1.0D, width), availableWidth);
        double anchorHeight = Math.min(Math.max(1.0D, height), availableHeight);
        double maxX = Math.max(marginLeft, screenWidth - marginRight - anchorWidth);
        double maxY = Math.max(marginTop, screenHeight - marginBottom - anchorHeight);
        double x = clamp(pointerX - anchorWidth / 2.0D, marginLeft, maxX);
        boolean abovePointer = target.y() + target.height() <= pointerY;
        double y = abovePointer
                ? target.y() + target.height() - anchorHeight
                : target.y();
        return new TooltipBounds(x, clamp(y, marginTop, maxY), anchorWidth, anchorHeight);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }
}
