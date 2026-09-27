package xin.vanilla.banira.client.notification;

import org.junit.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

public class NotificationMuteStateTest {
    @Test
    public void expiresExactlyAtFiveMinutes() {
        AtomicLong clock = new AtomicLong(-123L);
        NotificationMuteState state = new NotificationMuteState(clock::get);
        assertFalse(state.muted());
        state.muteForFiveMinutes();
        assertTrue(state.muted());
        clock.addAndGet(TimeUnit.MINUTES.toNanos(5) - 1);
        assertTrue(state.muted());
        clock.incrementAndGet();
        assertFalse(state.muted());
    }

    @Test
    public void newSessionIsNotMutedAndNewClickRestartsDuration() {
        AtomicLong clock = new AtomicLong(Long.MAX_VALUE - 5);
        NotificationMuteState state = new NotificationMuteState(clock::get);
        state.muteForFiveMinutes();
        clock.addAndGet(TimeUnit.MINUTES.toNanos(4));
        state.muteForFiveMinutes();
        clock.addAndGet(TimeUnit.MINUTES.toNanos(2));
        assertTrue(state.muted());
        assertFalse(new NotificationMuteState(clock::get).muted());
    }
}
