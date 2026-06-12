package xin.vanilla.banira.api.client.input;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class BaniraMouseClickTrackerTest {

    @Test
    public void detectsDoubleClickWithinWindow() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraMouseClickTracker tracker = new BaniraMouseClickTracker(300L, 5.0D, now::get);

        BaniraMouseClickTracker.Result first = tracker.record(10.0D, 20.0D, 0);
        assertEquals(1, first.clickCount());
        assertFalse(first.doubleClick());

        now.addAndGet(120L);
        BaniraMouseClickTracker.Result second = tracker.record(12.0D, 22.0D, 0);
        assertEquals(2, second.clickCount());
        assertTrue(second.doubleClick());
        assertTrue(second.repeatedClick());
    }

    @Test
    public void resetsWhenTimeDistanceOrButtonDiffers() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraMouseClickTracker tracker = new BaniraMouseClickTracker(300L, 5.0D, now::get);

        tracker.record(10.0D, 20.0D, 0);
        now.addAndGet(301L);
        assertEquals(1, tracker.record(10.0D, 20.0D, 0).clickCount());

        now.addAndGet(100L);
        assertEquals(1, tracker.record(30.0D, 20.0D, 0).clickCount());

        now.addAndGet(100L);
        assertEquals(1, tracker.record(30.0D, 20.0D, 1).clickCount());
    }
}
