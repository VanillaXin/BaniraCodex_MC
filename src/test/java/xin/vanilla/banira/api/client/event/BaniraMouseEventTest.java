package xin.vanilla.banira.api.client.event;

import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraDragTracker;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class BaniraMouseEventTest {

    @Test
    public void carriesClickMetadata() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraMouseClickTracker tracker = new BaniraMouseClickTracker(300L, 5.0D, now::get);
        BaniraScreenInfo screen = new BaniraScreenInfo("test.Screen", "Test", 320, 180, true);

        tracker.record(10.0D, 20.0D, 0);
        now.addAndGet(100L);

        BaniraMouseEvent event = BaniraMouseEvent.clicked(screen, 11.0D, 21.0D, 0)
                .withClickMetadata(tracker.record(11.0D, 21.0D, 0));

        assertEquals(2, event.clickCount());
        assertTrue(event.doubleClick());
        assertTrue(event.repeatedClick());
        assertTrue(event.clickTracked());
    }

    @Test
    public void carriesDragMetadata() {
        BaniraDragTracker tracker = new BaniraDragTracker();
        BaniraScreenInfo screen = new BaniraScreenInfo("test.Screen", "Test", 320, 180, true);

        tracker.press(10.0D, 20.0D, 0);
        BaniraMouseEvent event = BaniraMouseEvent.dragged(screen, 13.0D, 24.0D, 0, 3.0D, 4.0D)
                .withDragMetadata(tracker.drag(13.0D, 24.0D, 0, 3.0D, 4.0D));

        assertEquals(BaniraMouseEvent.Action.DRAG, event.action());
        assertTrue(event.dragging());
        assertTrue(event.dragStarted());
        assertFalse(event.dragEnded());
        assertTrue(event.dragTracked());
        assertEquals(10.0D, event.dragStartX(), 0.0001D);
        assertEquals(20.0D, event.dragStartY(), 0.0001D);
        assertEquals(3.0D, event.dragTotalX(), 0.0001D);
        assertEquals(4.0D, event.dragTotalY(), 0.0001D);
    }
}
