package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class UiPointerOwnershipTest {
    @Test public void staleDisposalCannotRestoreOverNewOwner() {
        UiPointerOwnership owners = new UiPointerOwnership(); Object old = new Object(), next = new Object();
        AtomicInteger hidden = new AtomicInteger(), restored = new AtomicInteger();
        owners.acquire(old, hidden::incrementAndGet); owners.acquire(next, hidden::incrementAndGet);
        owners.release(old, restored::incrementAndGet); assertEquals(0, restored.get());
        owners.release(next, restored::incrementAndGet); owners.release(next, restored::incrementAndGet);
        assertEquals(2, hidden.get()); assertEquals(1, restored.get());
    }
    @Test public void repeatedAcquireDoesNotRepeatSystemModeCalls() {
        UiPointerOwnership owners = new UiPointerOwnership(); Object token = new Object(); AtomicInteger modes = new AtomicInteger();
        owners.acquire(token, modes::incrementAndGet); owners.acquire(token, modes::incrementAndGet);
        assertEquals(1, modes.get());
    }
    @Test public void failedAcquisitionPreservesPreviousOwner() {
        UiPointerOwnership owners = new UiPointerOwnership(); Object old = new Object(); AtomicInteger restored = new AtomicInteger();
        owners.acquire(old, () -> {});
        try { owners.acquire(new Object(), () -> { throw new IllegalStateException("Mode failure"); }); fail(); }
        catch (IllegalStateException expected) { }
        owners.release(old, restored::incrementAndGet); assertEquals(1, restored.get());
    }
    @Test public void nullCannotAcquireOrReleaseAnOwner() {
        UiPointerOwnership owners = new UiPointerOwnership();
        try { owners.acquire(null, () -> fail()); fail(); } catch (IllegalArgumentException expected) { }
        owners.release(null, () -> fail());
    }
}
