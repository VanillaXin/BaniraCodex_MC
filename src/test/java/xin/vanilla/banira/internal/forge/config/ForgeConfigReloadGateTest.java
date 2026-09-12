package xin.vanilla.banira.internal.forge.config;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ForgeConfigReloadGateTest {
    @Test
    public void simultaneousWatchersNotifyOnceForOneExternalVersion() throws Exception {
        AtomicBoolean changed = new AtomicBoolean(true);
        AtomicInteger delivered = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ForgeConfigReloadGate gate = new ForgeConfigReloadGate(changed::get, () -> {
            entered.countDown();
            await(release);
            delivered.incrementAndGet();
            changed.set(false);
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> nativeWatch = workers.submit(gate);
            await(entered);
            Future<?> supplementalWatch = workers.submit(gate);
            release.countDown();
            nativeWatch.get(5, TimeUnit.SECONDS);
            supplementalWatch.get(5, TimeUnit.SECONDS);
            assertEquals(1, delivered.get());
        } finally {
            release.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void unloadWaitsForDeliveryAndRejectsAlreadyCapturedCallbacks() throws Exception {
        AtomicInteger delivered = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ForgeConfigReloadGate gate = new ForgeConfigReloadGate(() -> true, () -> {
            entered.countDown();
            await(release);
            delivered.incrementAndGet();
        });
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> running = workers.submit(gate);
            await(entered);
            CountDownLatch closing = new CountDownLatch(1);
            Future<?> closed = workers.submit(() -> { closing.countDown(); gate.close(); });
            await(closing);
            assertFalse(closed.isDone());
            release.countDown();
            running.get(5, TimeUnit.SECONDS);
            closed.get(5, TimeUnit.SECONDS);
            gate.run();
            assertEquals(1, delivered.get());
        } finally {
            release.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }
}
