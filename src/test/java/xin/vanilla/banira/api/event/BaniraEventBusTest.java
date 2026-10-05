package xin.vanilla.banira.api.event;

import org.junit.Test;
import xin.vanilla.banira.common.util.BaniraEventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BaniraEventBusTest {
    private static final Channel[] CHANNELS = {
            new Channel("starting", r -> BaniraEventBus.Server.onStarting(e -> r.run()), () -> BaniraEventBus.dispatchServerStarting(null)),
            new Channel("started", r -> BaniraEventBus.Server.onStarted(e -> r.run()), () -> BaniraEventBus.dispatchServerStarted(null)),
            new Channel("stopping", r -> BaniraEventBus.Server.onStopping(e -> r.run()), () -> BaniraEventBus.dispatchServerStopping(null)),
            new Channel("serverTick", r -> BaniraEventBus.Server.onTick(e -> r.run()), () -> BaniraEventBus.dispatchServerTick(null)),
            new Channel("login", r -> BaniraEventBus.Player.onLoggedIn(e -> r.run()), () -> BaniraEventBus.dispatchPlayerLoggedIn(null)),
            new Channel("logout", r -> BaniraEventBus.Player.onLoggedOut(e -> r.run()), () -> BaniraEventBus.dispatchPlayerLoggedOut(null)),
            new Channel("dimension", r -> BaniraEventBus.Player.onChangedDimension(e -> r.run()), () -> BaniraEventBus.dispatchPlayerChangedDimension(null)),
            new Channel("playerSave", r -> BaniraEventBus.Player.onSave(e -> r.run()), () -> BaniraEventBus.dispatchPlayerSave(null)),
            new Channel("worldSave", BaniraEventBus.Save::onWorldSave, BaniraEventBus::dispatchWorldSave),
            new Channel("chunkSave", BaniraEventBus.Save::onChunkSave, BaniraEventBus::dispatchChunkSave),
            new Channel("worldUnload", r -> BaniraEventBus.WorldEvents.onUnload(e -> r.run()), () -> BaniraEventBus.dispatchWorldUnload(null)),
            new Channel("worldTick", r -> BaniraEventBus.WorldEvents.onTick(e -> r.run()), () -> BaniraEventBus.dispatchWorldTick(null))
    };

    @Test
    public void parallelRegistrationPreservesEveryCallbackOnEveryChannel() throws Exception {
        int threads = 8, perThread = 256;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            for (Channel channel : CHANNELS) {
                CountDownLatch ready = new CountDownLatch(threads), start = new CountDownLatch(1);
                AtomicIntegerArray delivered = new AtomicIntegerArray(threads * perThread);
                ConcurrentLinkedQueue<BaniraEventRegistration> registrations = new ConcurrentLinkedQueue<>();
                List<Future<?>> futures = new ArrayList<>();
                try {
                    for (int worker = 0; worker < threads; worker++) {
                        final int offset = worker * perThread;
                        futures.add(executor.submit(() -> {
                            ready.countDown();
                            assertTrue(start.await(5, TimeUnit.SECONDS));
                            for (int i = 0; i < perThread; i++) {
                                final int index = offset + i;
                                registrations.add(channel.register.apply(() -> delivered.incrementAndGet(index)));
                            }
                            return null;
                        }));
                    }
                    assertTrue(ready.await(5, TimeUnit.SECONDS));
                    start.countDown();
                    for (Future<?> future : futures) future.get(15, TimeUnit.SECONDS);
                    channel.dispatch.run();
                    for (int i = 0; i < delivered.length(); i++) assertEquals(channel.name + " callback " + i, 1, delivered.get(i));
                } finally {
                    start.countDown();
                    for (Future<?> future : futures) {
                        if (!future.isDone()) future.cancel(true);
                    }
                    for (BaniraEventRegistration registration : registrations) registration.unregister();
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void changesDuringDispatchApplyToNextSnapshotWithoutLosingOrder() {
        List<String> calls = new ArrayList<>();
        List<BaniraEventRegistration> added = new ArrayList<>();
        BaniraEventRegistration[] second = new BaniraEventRegistration[1];
        BaniraEventRegistration first = BaniraEventBus.Save.onWorldSave(() -> {
            calls.add("first");
            second[0].unregister();
            added.add(BaniraEventBus.Save.onWorldSave(() -> calls.add("new")));
        });
        second[0] = BaniraEventBus.Save.onWorldSave(() -> calls.add("second"));
        try {
            BaniraEventBus.dispatchWorldSave();
            assertEquals(Arrays.asList("first", "second"), calls);
            calls.clear();
            BaniraEventBus.dispatchWorldSave();
            assertEquals(Arrays.asList("first", "new"), calls);
        } finally {
            first.unregister();
            second[0].unregister();
            for (BaniraEventRegistration registration : added) registration.unregister();
        }
    }

    @Test
    public void failureDoesNotSkipLaterCallbacksAndUnregisterRemainsEffective() {
        AtomicInteger calls = new AtomicInteger();
        BaniraEventRegistration failing = BaniraEventBus.Save.onChunkSave(() -> { throw new IllegalStateException("expected test failure"); });
        BaniraEventRegistration succeeding = BaniraEventBus.Save.onChunkSave(calls::incrementAndGet);
        try {
            BaniraEventBus.dispatchChunkSave();
            assertEquals(1, calls.get());
            succeeding.unregister();
            BaniraEventBus.dispatchChunkSave();
            assertEquals(1, calls.get());
        } finally {
            failing.unregister();
            succeeding.unregister();
        }
    }

    private static final class Channel {
        final String name;
        final Function<Runnable, BaniraEventRegistration> register;
        final Runnable dispatch;
        Channel(String name, Function<Runnable, BaniraEventRegistration> register, Runnable dispatch) {
            this.name = name;
            this.register = register;
            this.dispatch = dispatch;
        }
    }
}
