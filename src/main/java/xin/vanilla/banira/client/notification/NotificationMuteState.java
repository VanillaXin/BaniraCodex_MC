package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Client-session notification mute, unaffected by wall-clock changes.
 */
public final class NotificationMuteState {
    private static final int[] MINUTES = {5, 10, 30, 60, 120, 240, 480, 960};
    private static final long WINDOW = TimeUnit.MINUTES.toNanos(5);
    private final LongSupplier nanoClock;
    private final Map<String, Mute> byType = new HashMap<>();
    private boolean active;
    private long startedAt;

    private static final class Mute {
        private final long startedAt;
        private final int tier;

        private Mute(long startedAt, int tier) {
            this.startedAt = startedAt;
            this.tier = tier;
        }

        private long duration() {
            return TimeUnit.MINUTES.toNanos(MINUTES[tier]);
        }
    }

    public NotificationMuteState(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    public int mute(String typeId) {
        long now = nanoClock.getAsLong();
        byType.entrySet().removeIf(e -> now - e.getValue().startedAt >= e.getValue().duration() + WINDOW);
        String type = NotificationTypeKeys.normalizeOrDefault(typeId);
        Mute previous = byType.get(type);
        if (previous != null && now - previous.startedAt < previous.duration()) return MINUTES[previous.tier];
        int tier = nextTier(previous, now);
        byType.put(type, new Mute(now, tier));
        return MINUTES[tier];
    }

    public int nextMinutes(String typeId) {
        return MINUTES[nextTier(byType.get(NotificationTypeKeys.normalizeOrDefault(typeId)), nanoClock.getAsLong())];
    }

    private static int nextTier(Mute previous, long now) {
        if (previous == null) return 0;
        long elapsed = now - previous.startedAt;
        if (elapsed < previous.duration()) return previous.tier;
        if (elapsed < previous.duration() + WINDOW) return Math.min(previous.tier + 1, MINUTES.length - 1);
        return 0;
    }

    public boolean muted(String typeId) {
        String type = NotificationTypeKeys.normalizeOrDefault(typeId);
        Mute mute = byType.get(type);
        if (mute == null) return false;
        long elapsed = nanoClock.getAsLong() - mute.startedAt;
        if (elapsed >= mute.duration() + WINDOW) byType.remove(type);
        return elapsed < mute.duration();
    }

    public void muteForFiveMinutes() {
        startedAt = nanoClock.getAsLong();
        active = true;
    }

    public boolean muted() {
        if (active && nanoClock.getAsLong() - startedAt >= WINDOW) active = false;
        return active;
    }
}
