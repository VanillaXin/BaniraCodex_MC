package xin.vanilla.banira.client.gui.event;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class MouseClickTrackerTest {

    @Test
    public void detectsDoubleClickWithinWindow() {
        AtomicLong now = new AtomicLong(1000L);
        MouseClickTracker tracker = new MouseClickTracker(300L, 5.0D, now::get);

        MouseClickTracker.Result first = tracker.record(10.0D, 20.0D, 0);
        assertEquals(1, first.clickCount());
        assertFalse(first.doubleClick());

        now.addAndGet(120L);
        MouseClickTracker.Result second = tracker.record(12.0D, 22.0D, 0);
        assertEquals(2, second.clickCount());
        assertTrue(second.doubleClick());
        assertTrue(second.repeatedClick());
    }

    @Test
    public void resetsWhenTimeDistanceOrButtonDiffers() {
        AtomicLong now = new AtomicLong(1000L);
        MouseClickTracker tracker = new MouseClickTracker(300L, 5.0D, now::get);

        tracker.record(10.0D, 20.0D, 0);
        now.addAndGet(301L);
        assertEquals(1, tracker.record(10.0D, 20.0D, 0).clickCount());

        now.addAndGet(100L);
        assertEquals(1, tracker.record(30.0D, 20.0D, 0).clickCount());

        now.addAndGet(100L);
        assertEquals(1, tracker.record(30.0D, 20.0D, 1).clickCount());
    }

    @Test
    public void thirdClickIsRepeatedButNotDoubleClick() {
        AtomicLong now = new AtomicLong(1000L);
        MouseClickTracker tracker = new MouseClickTracker(300L, 5.0D, now::get);

        tracker.record(10.0D, 20.0D, 0);
        now.addAndGet(80L);
        tracker.record(10.0D, 20.0D, 0);
        now.addAndGet(80L);
        MouseClickTracker.Result third = tracker.record(10.0D, 20.0D, 0);

        assertEquals(3, third.clickCount());
        assertFalse(third.doubleClick());
        assertTrue(third.repeatedClick());
    }

    @Test
    public void mouseEventCarriesClickMetadata() {
        AtomicLong now = new AtomicLong(1000L);
        MouseClickTracker tracker = new MouseClickTracker(300L, 5.0D, now::get);
        tracker.record(1.0D, 2.0D, 0);
        now.addAndGet(100L);

        MouseEvent event = MouseEvent.of(1.0D, 2.0D, 0, tracker.record(1.0D, 2.0D, 0));

        assertEquals(2, event.clickCount());
        assertTrue(event.doubleClick());
        assertTrue(event.clickTracked());
    }
}
