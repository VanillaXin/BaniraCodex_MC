package xin.vanilla.banira.client.gui.event;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class KeyClickTrackerTest {

    @Test
    public void detectsDoublePressAfterRelease() {
        AtomicLong now = new AtomicLong(1000L);
        KeyClickTracker tracker = new KeyClickTracker(300L, now::get);

        KeyClickTracker.Result first = tracker.recordPress(65, 30, 0);
        assertEquals(1, first.pressCount());
        assertFalse(first.doublePress());
        tracker.recordRelease(65, 30, 0);

        now.addAndGet(120L);
        KeyClickTracker.Result second = tracker.recordPress(65, 30, 0);
        assertEquals(2, second.pressCount());
        assertTrue(second.doublePress());
        assertTrue(second.repeatedPress());
    }

    @Test
    public void heldRepeatDoesNotCountAsDoublePress() {
        AtomicLong now = new AtomicLong(1000L);
        KeyClickTracker tracker = new KeyClickTracker(300L, now::get);

        tracker.recordPress(65, 30, 0);
        now.addAndGet(80L);
        KeyClickTracker.Result repeat = tracker.recordPress(65, 30, 0);

        assertEquals(1, repeat.pressCount());
        assertTrue(repeat.heldRepeat());
        assertFalse(repeat.doublePress());
        assertFalse(repeat.repeatedPress());
    }

    @Test
    public void resetsWhenTimeOrModifiersDiffer() {
        AtomicLong now = new AtomicLong(1000L);
        KeyClickTracker tracker = new KeyClickTracker(300L, now::get);

        tracker.recordPress(65, 30, 0);
        tracker.recordRelease(65, 30, 0);
        now.addAndGet(301L);
        assertEquals(1, tracker.recordPress(65, 30, 0).pressCount());
        tracker.recordRelease(65, 30, 0);

        now.addAndGet(100L);
        assertEquals(1, tracker.recordPress(65, 30, 2).pressCount());
    }

    @Test
    public void releaseIgnoresModifierStateForHeldTracking() {
        AtomicLong now = new AtomicLong(1000L);
        KeyClickTracker tracker = new KeyClickTracker(300L, now::get);

        tracker.recordPress(65, 30, 2);
        tracker.recordRelease(65, 30, 0);
        now.addAndGet(100L);

        assertFalse(tracker.recordPress(65, 30, 2).heldRepeat());
    }

    @Test
    public void keyEventCarriesPressMetadata() {
        AtomicLong now = new AtomicLong(1000L);
        KeyClickTracker tracker = new KeyClickTracker(300L, now::get);

        tracker.recordPress(65, 30, 0);
        tracker.recordRelease(65, 30, 0);
        now.addAndGet(100L);

        KeyEvent event = KeyEvent.of(65, 30, 0, tracker.recordPress(65, 30, 0));

        assertEquals(65, event.key());
        assertEquals(2, event.pressCount());
        assertTrue(event.doublePress());
        assertTrue(event.repeatedPress());
        assertFalse(event.heldRepeat());
        assertTrue(event.pressTracked());
    }
}
