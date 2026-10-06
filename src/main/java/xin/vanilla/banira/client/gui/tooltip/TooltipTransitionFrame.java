package xin.vanilla.banira.client.gui.tooltip;

/**
 * 一帧需要绘制的 Tooltip 动画结果。
 */
public final class TooltipTransitionFrame<K> {
    private final TooltipBounds bounds;
    private final K contentKey;
    private final double progress;

    TooltipTransitionFrame(TooltipBounds bounds, K contentKey, double progress) {
        this.bounds = bounds;
        this.contentKey = contentKey;
        this.progress = progress;
    }

    public TooltipBounds bounds() {
        return bounds;
    }

    public K contentKey() {
        return contentKey;
    }

    public double progress() {
        return progress;
    }
}
