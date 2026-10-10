package xin.vanilla.banira.client.event;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ClientSetupOrderingTest {
    @Before @After public void reset() throws Exception {
        Field callbacks = BaniraClientEventHub.class.getDeclaredField("modClientSetupCallbacks");
        callbacks.setAccessible(true);
        ((List<?>) callbacks.get(null)).clear();
        try {
            Field fired = BaniraClientEventHub.class.getDeclaredField("modClientSetupEvent");
            fired.setAccessible(true); fired.set(null, null);
        } catch (NoSuchFieldException beforeFix) { }
    }
    @Test public void lateSubscriberReceivesOriginalSetupExactlyOnce() {
        BaniraClientSetupEvent event = new BaniraClientSetupEvent();
        BaniraClientEventHub.dispatchModClientSetup(event);
        AtomicInteger calls = new AtomicInteger();
        BaniraClientEventHub.ModLifecycle.onClientSetup(seen -> { assertSame(event, seen); calls.incrementAndGet(); });
        assertEquals(1, calls.get());
        BaniraClientEventHub.dispatchModClientSetup(new BaniraClientSetupEvent());
        assertEquals(1, calls.get());
    }
    @Test public void earlySubscriberDoesNotReceiveDuplicateDispatch() {
        AtomicInteger calls = new AtomicInteger();
        BaniraClientEventHub.ModLifecycle.onClientSetup(event -> calls.incrementAndGet());
        BaniraClientEventHub.dispatchModClientSetup();
        BaniraClientEventHub.dispatchModClientSetup();
        assertEquals(1, calls.get());
    }
    @Test public void reentrantSubscriptionRunsOnceWithoutMutatingDispatchIterator() {
        AtomicInteger parent = new AtomicInteger(), child = new AtomicInteger();
        BaniraClientEventHub.ModLifecycle.onClientSetup(event -> {
            parent.incrementAndGet();
            BaniraClientEventHub.ModLifecycle.onClientSetup(late -> child.incrementAndGet());
        });
        BaniraClientEventHub.dispatchModClientSetup();
        assertEquals(1, parent.get());
        assertEquals(1, child.get());
    }
}
