package xin.vanilla.banira.internal.forge.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class ForgeConfigWatchTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void coalescedWritesAndAtomicReplacementReachRegisteredFile() throws Exception {
        Path target = temporary.newFile("managed.toml").toPath();
        AtomicReference<CountDownLatch> received = new AtomicReference<>(new CountDownLatch(1));
        AtomicReference<String> expected = new AtomicReference<>("value = 20");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (ForgeConfigWatch watch = new ForgeConfigWatch(failure::set)) {
            watch.watch(target, () -> {
                try {
                    if (expected.get().equals(new String(Files.readAllBytes(target), StandardCharsets.UTF_8))) received.get().countDown();
                } catch (Exception error) { failure.set(error); }
            });
            for (int n = 0; n <= 20; n++) Files.write(target, ("value = " + n).getBytes(StandardCharsets.UTF_8));
            assertTrue(received.get().await(5, TimeUnit.SECONDS));
            expected.set("value = 321");
            received.set(new CountDownLatch(1));
            Path replacement = temporary.newFile("staging.tmp").toPath();
            Files.write(replacement, expected.get().getBytes(StandardCharsets.UTF_8));
            Files.move(replacement, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            assertTrue(received.get().await(5, TimeUnit.SECONDS));
            assertNull(failure.get());
        }
    }

    @Test
    public void unloadingOneFilePreservesOtherWatchesInSameDirectory() throws Exception {
        Path first = temporary.newFile("first.toml").toPath();
        Path second = temporary.newFile("second.toml").toPath();
        CountDownLatch changed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try (ForgeConfigWatch watch = new ForgeConfigWatch(failure::set)) {
            watch.watch(first, () -> failure.set(new AssertionError("Unloaded file callback")));
            watch.watch(second, changed::countDown);
            watch.unwatch(first);
            Files.write(first, new byte[]{1});
            Files.write(second, new byte[]{2});
            assertTrue(changed.await(5, TimeUnit.SECONDS));
            assertNull(failure.get());
        }
    }
}
