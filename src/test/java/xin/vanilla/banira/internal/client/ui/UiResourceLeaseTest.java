package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class UiResourceLeaseTest {
    @Test
    public void releasedBindingsDoNotAccumulateDuringInPlaceRefresh() throws Exception {
        UiOwnedResources resources = new UiOwnedResources();
        AtomicInteger closed = new AtomicInteger();
        var field = UiOwnedResources.class.getDeclaredField("resources");
        field.setAccessible(true);
        for (int i = 0; i < 100; i++) {
            AutoCloseable lease = resources.own(closed::incrementAndGet);
            lease.close();
            lease.close();
            assertEquals(i + 1, closed.get());
            assertEquals("Released callbacks must not stay retained until the document closes", 0, ((Deque<?>) field.get(resources)).size());
        }
        resources.close();
        assertEquals(100, closed.get());
    }

    @Test
    public void ownerCloseAndLeaseCloseReleaseUnderlyingResourceOnce() throws Exception {
        UiOwnedResources resources = new UiOwnedResources();
        AtomicInteger closed = new AtomicInteger();
        AutoCloseable lease = resources.own(closed::incrementAndGet);
        resources.close();
        lease.close();
        resources.close();
        assertEquals(1, closed.get());
    }
}
