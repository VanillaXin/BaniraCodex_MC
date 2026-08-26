package xin.vanilla.banira.client.gui.tooltip;

import java.util.Objects;

/**
 * 与渲染 API 无关的 Tooltip 过渡状态，使用纳秒时间保证不同帧率下速度一致。
 */
public final class TooltipTransitionModel<K> {
    private final long durationNanos;
    private final long continuityNanos;
    private final double maxPointerDistanceSquared;
    private final double contentSwitchProgress;

    private TooltipBounds startBounds;
    private TooltipBounds targetBounds;
    private K previousContentKey;
    private K targetContentKey;
    private long transitionStartedAt;
    private long missingSince = Long.MIN_VALUE;
    private double lastPointerX = Double.NaN;
    private double lastPointerY = Double.NaN;
    private boolean initialized;
    private boolean collapsingMissing;

    public TooltipTransitionModel(long durationNanos, long continuityNanos, double contentSwitchProgress) {
        this(durationNanos, continuityNanos, Double.POSITIVE_INFINITY, contentSwitchProgress);
    }

    public TooltipTransitionModel(long durationNanos, long continuityNanos,
                                  double maxPointerDistance, double contentSwitchProgress) {
        this.durationNanos = Math.max(1L, durationNanos);
        this.continuityNanos = Math.max(0L, continuityNanos);
        double distance = Math.max(0.0D, maxPointerDistance);
        this.maxPointerDistanceSquared = distance * distance;
        this.contentSwitchProgress = Math.max(0.0D, Math.min(1.0D, contentSwitchProgress));
    }

    public TooltipTransitionFrame<K> resolve(K contentKey, TooltipBounds bounds, long nowNanos) {
        return resolve(contentKey, bounds, lastPointerX, lastPointerY, nowNanos);
    }

    public TooltipTransitionFrame<K> resolve(K contentKey, TooltipBounds bounds,
                                             double pointerX, double pointerY, long nowNanos) {
        boolean continuityExpired = continuityExpired(nowNanos) || pointerMovedTooFar(pointerX, pointerY);
        if (!initialized || continuityExpired) {
            initialize(contentKey, bounds, pointerX, pointerY, nowNanos);
            return currentFrame(nowNanos);
        }
        missingSince = Long.MIN_VALUE;
        collapsingMissing = false;

        TooltipTransitionFrame<K> current = currentFrame(nowNanos);
        if (!Objects.equals(targetContentKey, contentKey)) {
            startBounds = current.bounds();
            targetBounds = bounds;
            previousContentKey = current.contentKey();
            targetContentKey = contentKey;
            transitionStartedAt = nowNanos;
            rememberPointer(pointerX, pointerY);
            return frame(startBounds, previousContentKey, 0.0D);
        }

        updateMovingTarget(bounds, current, nowNanos);
        rememberPointer(pointerX, pointerY);
        return currentFrame(nowNanos);
    }

    /** 离开 Tooltip 区域后向靠近鼠标的水平边缘收缩。 */
    public TooltipTransitionFrame<K> resolveMissing(double pointerX, double pointerY, long nowNanos) {
        if (!initialized) return null;
        if (missingSince == Long.MIN_VALUE) missingSince = nowNanos;
        if (continuityExpired(nowNanos) || pointerMovedTooFar(pointerX, pointerY)) {
            reset();
            return null;
        }
        if (!collapsingMissing) {
            TooltipTransitionFrame<K> current = currentFrame(nowNanos);
            startBounds = current.bounds();
            targetBounds = current.bounds().collapseToVerticalEdge(pointerY);
            previousContentKey = current.contentKey();
            targetContentKey = current.contentKey();
            transitionStartedAt = nowNanos;
            collapsingMissing = true;
            return frame(startBounds, previousContentKey, 0.0D);
        }
        return currentFrame(nowNanos);
    }

    public void markMissing(long nowNanos) {
        if (initialized && missingSince == Long.MIN_VALUE) {
            missingSince = nowNanos;
        }
    }

    public void reset() {
        initialized = false;
        startBounds = null;
        targetBounds = null;
        previousContentKey = null;
        targetContentKey = null;
        transitionStartedAt = 0L;
        missingSince = Long.MIN_VALUE;
        lastPointerX = Double.NaN;
        lastPointerY = Double.NaN;
        collapsingMissing = false;
    }

    private void initialize(K contentKey, TooltipBounds bounds,
                            double pointerX, double pointerY, long nowNanos) {
        startBounds = pointerAvailable(pointerX, pointerY)
                ? bounds.collapseToVerticalEdge(pointerY)
                : bounds;
        targetBounds = bounds;
        previousContentKey = contentKey;
        targetContentKey = contentKey;
        transitionStartedAt = pointerAvailable(pointerX, pointerY)
                ? nowNanos
                : nowNanos - durationNanos;
        missingSince = Long.MIN_VALUE;
        initialized = true;
        collapsingMissing = false;
        rememberPointer(pointerX, pointerY);
    }

    private boolean continuityExpired(long nowNanos) {
        return missingSince != Long.MIN_VALUE && nowNanos - missingSince > continuityNanos;
    }

    private boolean pointerMovedTooFar(double pointerX, double pointerY) {
        if (missingSince == Long.MIN_VALUE || Double.isNaN(lastPointerX) || Double.isNaN(lastPointerY)
                || Double.isNaN(pointerX) || Double.isNaN(pointerY)) return false;
        double dx = pointerX - lastPointerX;
        double dy = pointerY - lastPointerY;
        return dx * dx + dy * dy > maxPointerDistanceSquared;
    }

    private void rememberPointer(double pointerX, double pointerY) {
        if (pointerAvailable(pointerX, pointerY)) {
            lastPointerX = pointerX;
            lastPointerY = pointerY;
        }
    }

    private boolean pointerAvailable(double pointerX, double pointerY) {
        return !Double.isNaN(pointerX) && !Double.isNaN(pointerY);
    }

    private void updateMovingTarget(TooltipBounds bounds, TooltipTransitionFrame<K> current, long nowNanos) {
        double dx = bounds.x() - targetBounds.x();
        double dy = bounds.y() - targetBounds.y();
        boolean sizeChanged = Double.compare(bounds.width(), targetBounds.width()) != 0
                || Double.compare(bounds.height(), targetBounds.height()) != 0;
        if (sizeChanged) {
            startBounds = current.bounds();
            targetBounds = bounds;
            transitionStartedAt = nowNanos;
            previousContentKey = current.contentKey();
        } else if (dx != 0.0D || dy != 0.0D) {
            startBounds = startBounds.translate(dx, dy);
            targetBounds = bounds;
        }
    }

    private TooltipTransitionFrame<K> currentFrame(long nowNanos) {
        double linear = Math.max(0.0D, Math.min(1.0D,
                (nowNanos - transitionStartedAt) / (double) durationNanos));
        double eased = 1.0D - Math.pow(1.0D - linear, 3.0D);
        TooltipBounds currentBounds = startBounds.interpolate(targetBounds, eased);
        K visibleContent = linear < contentSwitchProgress ? previousContentKey : targetContentKey;
        if (linear >= 1.0D) {
            startBounds = targetBounds;
            previousContentKey = targetContentKey;
        }
        return frame(currentBounds, visibleContent, linear);
    }

    private TooltipTransitionFrame<K> frame(TooltipBounds bounds, K contentKey, double progress) {
        return new TooltipTransitionFrame<>(bounds, contentKey, progress);
    }
}
