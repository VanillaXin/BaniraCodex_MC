package xin.vanilla.banira.client.gui.component;

/**
 * 分离通知生命周期与重复合并反馈，避免合并时重放完整入场动画。
 */
final class NotificationAnimationState {

    private static final long MERGE_PULSE_DURATION_MS = 180L;

    private long startedAt = -1L;
    private long holdUntil = -1L;
    private long mergeStartedAt = -1L;
    private double mergeStartVisibility = 1.0D;

    boolean started() {
        return startedAt >= 0L;
    }

    void start(long nowMs, long animationTimeMs, long durationTimeMs) {
        if (started()) {
            return;
        }
        startedAt = nowMs;
        holdUntil = nowMs + nonNegative(animationTimeMs) + nonNegative(durationTimeMs);
    }

    void merge(long nowMs, long durationTimeMs, long animationTimeMs) {
        if (!started()) {
            return;
        }
        double currentVisibility = visibility(nowMs, animationTimeMs);
        holdUntil = Math.max(holdUntil, nowMs + nonNegative(durationTimeMs));

        // 首次入场尚未完成时只续期，不叠加第二套动画。
        if (nowMs < startedAt + nonNegative(animationTimeMs)) {
            return;
        }
        mergeStartVisibility = clamp01(Math.max(0.0D, currentVisibility));
        mergeStartedAt = nowMs;
    }

    double visibility(long nowMs, long animationTimeMs) {
        if (!started()) {
            return 0.0D;
        }
        double lifecycle = lifecycleVisibility(nowMs, animationTimeMs);
        if (mergeStartedAt < 0L || lifecycle < 0.0D || mergeStartVisibility >= lifecycle) {
            return lifecycle;
        }
        double progress = mergeProgress(nowMs);
        if (progress >= 1.0D) {
            return lifecycle;
        }
        double eased = 1.0D - Math.pow(1.0D - progress, 3.0D);
        return mergeStartVisibility + (lifecycle - mergeStartVisibility) * eased;
    }

    double mergeEmphasis(long nowMs) {
        if (mergeStartedAt < 0L) {
            return 0.0D;
        }
        double progress = mergeProgress(nowMs);
        if (progress <= 0.0D || progress >= 1.0D) {
            return 0.0D;
        }
        return Math.sin(Math.PI * progress);
    }

    private double lifecycleVisibility(long nowMs, long animationTimeMs) {
        long animation = nonNegative(animationTimeMs);
        if (animation > 0L && nowMs < startedAt + animation) {
            return clamp01((nowMs - startedAt) / (double) animation);
        }
        if (nowMs < holdUntil) {
            return 1.0D;
        }
        if (animation == 0L) {
            return -1.0D;
        }
        long exitElapsed = nowMs - holdUntil;
        if (exitElapsed > animation) {
            return -1.0D;
        }
        return 1.0D - exitElapsed / (double) animation;
    }

    private double mergeProgress(long nowMs) {
        return clamp01((nowMs - mergeStartedAt) / (double) MERGE_PULSE_DURATION_MS);
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
