package xin.vanilla.banira.internal.dev;

/**
 * Shared pacing rules for the bounded server and client smoke workloads.
 */
public final class BaniraNetworkSmokeProfilePlan {
    public static final int MINIMUM_CYCLES = 20;
    public static final int CYCLE_INTERVAL_TICKS = 10;

    private BaniraNetworkSmokeProfilePlan() {
    }

    public static boolean shouldContinue(boolean sparkReportWritten, int completedCycles) {
        return !sparkReportWritten || completedCycles < MINIMUM_CYCLES;
    }

    public static boolean isCycleDue(int currentTick, int previousCycleTick) {
        return currentTick - previousCycleTick >= CYCLE_INTERVAL_TICKS;
    }

    public static boolean shouldScheduleConfigReload(boolean reloadPending, int cycle) {
        return !reloadPending && (cycle & 1) == 0;
    }
}
