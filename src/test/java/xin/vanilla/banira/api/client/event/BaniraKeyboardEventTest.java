package xin.vanilla.banira.api.client.event;

import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraKeyPressTracker;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class BaniraKeyboardEventTest {

    @Test
    public void carriesPressMetadata() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraKeyPressTracker tracker = new BaniraKeyPressTracker(300L, now::get);
        Object screen = new Object();
        Object nativeEvent = new Object();

        tracker.recordPress(65, 30, 0);
        tracker.recordRelease(65, 30);
        now.addAndGet(100L);

        BaniraKeyboardEvent event = BaniraKeyboardEvent.pressed(screen, 65, 30, 0, nativeEvent)
                .withPressMetadata(tracker.recordPress(65, 30, 0));

        assertEquals(2, event.pressCount());
        assertTrue(event.doublePress());
        assertTrue(event.repeatedPress());
        assertFalse(event.heldRepeat());
        assertTrue(event.pressTracked());
    }

    @Test
    public void carriesHeldRepeatMetadata() {
        BaniraKeyPressTracker tracker = new BaniraKeyPressTracker(300L, () -> 1000L);
        Object screen = new Object();
        Object nativeEvent = new Object();

        tracker.recordPress(65, 30, 0);
        BaniraKeyboardEvent event = BaniraKeyboardEvent.pressed(screen, 65, 30, 0, nativeEvent)
                .withPressMetadata(tracker.recordPress(65, 30, 0));

        assertEquals(1, event.pressCount());
        assertTrue(event.heldRepeat());
        assertFalse(event.doublePress());
        assertFalse(event.repeatedPress());
        assertTrue(event.pressTracked());
    }
}
