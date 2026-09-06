package xin.vanilla.banira.internal.server.dev;

/** Controls the bounded sustained segment of the dev-only server smoke. */
final class BaniraNetworkSmokeWorkload {
    static final int DURATION_TICKS = 320;

    private final int startedAt;

    BaniraNetworkSmokeWorkload(int startedAt) {
        this.startedAt = startedAt;
    }

    boolean completeAt(int currentTick) {
        return currentTick - startedAt >= DURATION_TICKS;
    }

    boolean shouldPollConfigAt(int currentTick) {
        return currentTick > startedAt && (currentTick - startedAt) % 20 == 0;
    }

    int elapsedTicksAt(int currentTick) {
        return Math.max(0, currentTick - startedAt);
    }
}
