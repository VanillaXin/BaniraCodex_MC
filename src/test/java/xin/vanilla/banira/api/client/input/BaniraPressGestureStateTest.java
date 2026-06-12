package xin.vanilla.banira.api.client.input;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class BaniraPressGestureStateTest {

    @Test
    public void progressAndReadyFollowPressDuration() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraPressGestureState state = new BaniraPressGestureState(now::get);

        assertFalse(state.pressing());
        assertEquals(0.0F, state.progress(200L), 0.0001F);

        state.press(0);
        assertTrue(state.pressing(0));
        assertFalse(state.ready(200L));

        now.addAndGet(100L);
        assertEquals(100L, state.elapsedMillis());
        assertEquals(0.5F, state.progress(200L), 0.0001F);

        now.addAndGet(100L);
        assertTrue(state.ready(200L));
        assertEquals(1.0F, state.progress(200L), 0.0001F);
    }

    @Test
    public void firePreventsRepeatedReadyUntilNextPress() {
        AtomicLong now = new AtomicLong(1000L);
        BaniraPressGestureState state = new BaniraPressGestureState(now::get);

        state.press(0);
        now.addAndGet(300L);
        assertTrue(state.ready(200L));

        state.fire();
        assertTrue(state.fired());
        assertFalse(state.ready(200L));
        assertEquals(1.0F, state.progress(200L), 0.0001F);
    }
}
