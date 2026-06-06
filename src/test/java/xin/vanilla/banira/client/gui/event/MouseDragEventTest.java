package xin.vanilla.banira.client.gui.event;

import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraDragTracker;

import static org.junit.Assert.*;

public class MouseDragEventTest {

    @Test
    public void carriesSharedDragMetadata() {
        BaniraDragTracker tracker = new BaniraDragTracker();
        tracker.press(10.0D, 20.0D, 0);

        MouseDragEvent event = MouseDragEvent.of(
                14.0D,
                25.0D,
                0,
                4.0D,
                5.0D,
                tracker.drag(14.0D, 25.0D, 0, 4.0D, 5.0D)
        );

        assertTrue(event.dragging());
        assertTrue(event.dragStarted());
        assertFalse(event.dragEnded());
        assertTrue(event.dragTracked());
        assertEquals(10.0D, event.dragStartX(), 0.0001D);
        assertEquals(20.0D, event.dragStartY(), 0.0001D);
        assertEquals(4.0D, event.dragTotalX(), 0.0001D);
        assertEquals(5.0D, event.dragTotalY(), 0.0001D);
    }
}
