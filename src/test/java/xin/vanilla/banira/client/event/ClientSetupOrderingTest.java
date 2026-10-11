package xin.vanilla.banira.client.event;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ClientSetupOrderingTest {
    @Before @After public void reset() throws Exception {
        Field callbacks = BaniraClientEvents.class.getDeclaredField("CLIENT_SETUP");
        callbacks.setAccessible(true);
        ((List<?>) callbacks.get(null)).clear();
        try {
            Field fired = BaniraClientEvents.class.getDeclaredField("clientSetupEvent");
            fired.setAccessible(true); fired.set(null, null);
        } catch (NoSuchFieldException beforeFix) { }
    }
    @Test public void lateSubscriberReceivesOriginalSetupExactlyOnce() {
        BaniraClientSetupEvent event = new BaniraClientSetupEvent();
        BaniraClientEvents.dispatchModClientSetup(event);
        AtomicInteger calls = new AtomicInteger();
        BaniraClientEvents.ModLifecycle.onClientSetup(seen -> { assertSame(event, seen); calls.incrementAndGet(); });
        assertEquals(1, calls.get());
        BaniraClientEvents.dispatchModClientSetup(new BaniraClientSetupEvent());
        assertEquals(1, calls.get());
    }
    @Test public void earlySubscriberDoesNotReceiveDuplicateDispatch() {
        AtomicInteger calls = new AtomicInteger();
        BaniraClientEvents.ModLifecycle.onClientSetup(event -> calls.incrementAndGet());
        BaniraClientEvents.dispatchModClientSetup(new BaniraClientSetupEvent());
        BaniraClientEvents.dispatchModClientSetup(new BaniraClientSetupEvent());
        assertEquals(1, calls.get());
    }
    @Test public void reentrantSubscriptionRunsOnceWithoutMutatingDispatchIterator() {
        AtomicInteger parent = new AtomicInteger(), child = new AtomicInteger();
        BaniraClientEvents.ModLifecycle.onClientSetup(event -> {
            parent.incrementAndGet();
            BaniraClientEvents.ModLifecycle.onClientSetup(late -> child.incrementAndGet());
        });
        BaniraClientEvents.dispatchModClientSetup(new BaniraClientSetupEvent());
        assertEquals(1, parent.get());
        assertEquals(1, child.get());
    }
}
