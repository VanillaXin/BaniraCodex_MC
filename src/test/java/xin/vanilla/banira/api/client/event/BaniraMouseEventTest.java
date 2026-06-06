package xin.vanilla.banira.api.client.event;

import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaniraMouseEventTest {

    @Test
    public void carriesClickMetadata() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraMouseClickTracker tracker = new BaniraMouseClickTracker(300L, 5.0D, now::get);
        Object screen = new Object();
        Object nativeEvent = new Object();

        tracker.record(10.0D, 20.0D, 0);
        now.addAndGet(100L);

        BaniraMouseEvent event = BaniraMouseEvent.clicked(screen, 11.0D, 21.0D, 0, nativeEvent)
                .withClickMetadata(tracker.record(11.0D, 21.0D, 0));

        assertEquals(2, event.clickCount());
        assertTrue(event.doubleClick());
        assertTrue(event.repeatedClick());
        assertTrue(event.clickTracked());
    }
}
