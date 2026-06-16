package xin.vanilla.banira.api.client.input;

import org.junit.Test;

import static org.junit.Assert.*;

public class BaniraDragTrackerTest {

    @Test
    public void firstDragStartsAndNextDragMoves() {
        BaniraDragTracker tracker = new BaniraDragTracker();
        tracker.press(10.0D, 20.0D, 0);

        BaniraDragTracker.Result first = tracker.drag(13.0D, 24.0D, 0, 3.0D, 4.0D);
        assertTrue(first.dragging());
        assertTrue(first.dragStarted());
        assertFalse(first.dragEnded());
        assertEquals(10.0D, first.startX(), 0.0001D);
        assertEquals(20.0D, first.startY(), 0.0001D);
        assertEquals(3.0D, first.totalX(), 0.0001D);
        assertEquals(4.0D, first.totalY(), 0.0001D);

        BaniraDragTracker.Result next = tracker.drag(18.0D, 25.0D, 0, 5.0D, 1.0D);
        assertTrue(next.dragging());
        assertFalse(next.dragStarted());
        assertEquals(8.0D, next.totalX(), 0.0001D);
        assertEquals(5.0D, next.totalY(), 0.0001D);
    }

    @Test
    public void releaseEndsActiveDrag() {
        BaniraDragTracker tracker = new BaniraDragTracker();
        tracker.press(10.0D, 20.0D, 0);
        tracker.drag(13.0D, 24.0D, 0, 3.0D, 4.0D);

        BaniraDragTracker.Result end = tracker.release(15.0D, 27.0D, 0);
        assertFalse(end.dragging());
        assertFalse(end.dragStarted());
        assertTrue(end.dragEnded());
        assertEquals(5.0D, end.totalX(), 0.0001D);
        assertEquals(7.0D, end.totalY(), 0.0001D);
    }
}
