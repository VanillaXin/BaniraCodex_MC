package xin.vanilla.banira.client.notification;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Client-session overlay mute, unaffected by wall-clock changes.
 */
public final class NotificationMuteState {
    private final LongSupplier nanoClock;
    private boolean active;
    private long startedAt;

    public NotificationMuteState(LongSupplier nanoClock) {
        this.nanoClock = nanoClock;
    }

    public void muteForFiveMinutes() {
        startedAt = nanoClock.getAsLong();
        active = true;
    }

    public boolean muted() {
        if (active && nanoClock.getAsLong() - startedAt >= TimeUnit.MINUTES.toNanos(5)) active = false;
        return active;
    }
}
