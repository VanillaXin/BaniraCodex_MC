package xin.vanilla.banira.api.client.input;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class BaniraKeyPressTrackerTest {

    @Test
    public void detectsDoublePressAfterRelease() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraKeyPressTracker tracker = new BaniraKeyPressTracker(300L, now::get);

        BaniraKeyPressTracker.Result first = tracker.recordPress(65, 30, 0);
        assertEquals(1, first.pressCount());
        assertFalse(first.doublePress());
        tracker.recordRelease(65, 30);

        now.addAndGet(120L);
        BaniraKeyPressTracker.Result second = tracker.recordPress(65, 30, 0);
        assertEquals(2, second.pressCount());
        assertTrue(second.doublePress());
        assertTrue(second.repeatedPress());
    }

    @Test
    public void heldRepeatDoesNotCountAsDoublePress() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraKeyPressTracker tracker = new BaniraKeyPressTracker(300L, now::get);

        tracker.recordPress(65, 30, 0);
        now.addAndGet(80L);
        BaniraKeyPressTracker.Result repeat = tracker.recordPress(65, 30, 0);

        assertEquals(1, repeat.pressCount());
        assertTrue(repeat.heldRepeat());
        assertFalse(repeat.doublePress());
        assertFalse(repeat.repeatedPress());
    }
}
