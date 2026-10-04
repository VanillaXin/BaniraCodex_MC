package xin.vanilla.banira.internal.script;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class ScriptCompileQueueTest {
    @Test public void coalescesBySessionAndRejectsSeventeenthWaitingSession() {
        List<Runnable> worker = new ArrayList<>();
        ScriptCompileQueue queue = new ScriptCompileQueue(worker::add, 16);
        List<Integer> ran = new ArrayList<>();
        List<Integer> cancelled = new ArrayList<>();
        Object first = new Object();
        queue.submit(first, () -> ran.add(-1), () -> cancelled.add(-1));
        queue.submit(first, () -> ran.add(0), () -> cancelled.add(0));
        assertEquals(Collections.singletonList(-1), cancelled);
        for (int i = 1; i < 16; i++) {
            final int n = i;
            queue.submit(new Object(), () -> ran.add(n), () -> cancelled.add(n));
        }
        try { queue.submit(new Object(), () -> fail("Overflow executed"), () -> {}); fail("Queue unbounded"); }
        catch (RejectedExecutionException expected) { }
        assertEquals(1, worker.size());
        worker.get(0).run();
        assertEquals(16, ran.size());
        assertFalse(ran.contains(-1));
    }

    @Test public void cancellationReleasesPendingSlotAndTaskFailureDoesNotStallQueue() {
        List<Runnable> worker = new ArrayList<>();
        ScriptCompileQueue queue = new ScriptCompileQueue(worker::add, 1);
        Object key = new Object();
        queue.submit(key, () -> fail("Cancelled work ran"), () -> {});
        queue.cancel(key);
        queue.submit(new Object(), () -> { throw new IllegalStateException("expected"); }, () -> {});
        worker.remove(0).run();
        List<Boolean> ran = new ArrayList<>();
        queue.submit(new Object(), () -> ran.add(true), () -> {});
        worker.remove(0).run();
        assertEquals(Collections.singletonList(true), ran);
    }
}
