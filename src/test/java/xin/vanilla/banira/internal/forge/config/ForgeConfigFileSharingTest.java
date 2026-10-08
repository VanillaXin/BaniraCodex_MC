package xin.vanilla.banira.internal.forge.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingException;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigCommitResult;
import xin.vanilla.banira.common.config.ConfigEditSnapshot;

import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ForgeConfigFileSharingTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void firstLoadCorrectionWaitsForShortWindowsSharingLease() throws Exception {
        Path path = file();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch writing = new CountDownLatch(1);
        try (ForgeConfigFile config = config(path, writing, true);
             FileChannel lease = lease(path)) {
            Future<?> result = worker.submit(config::load);
            assertTrue(writing.await(5, TimeUnit.SECONDS));
            Thread.sleep(75);
            assertFalse("A short delete-sharing lease must not abort startup", result.isDone());
            assertEquals("a = 1\n", text(path));
            lease.close();
            result.get(5, TimeUnit.SECONDS);
            assertEquals(2, ((Number) config.get("a")).intValue());
            assertTrue(text(path).contains("a = 2"));
        } finally { stop(worker); }
    }

    @Test public void persistentSharingDenialPreservesOriginalAndMemory() throws Exception {
        Path path = file();
        try (ForgeConfigFile config = config(path, new CountDownLatch(1), false)) {
            config.load();
            ConfigEditSnapshot snapshot = config.snapshotForEdit(Collections.singleton("a"));
            try (FileChannel ignored = lease(path)) {
                long start = System.nanoTime();
                assertThrows(WritingException.class, () -> config.compareAndSetAll(snapshot, Collections.singletonMap("a", 2)));
                assertTrue("Sharing retries must remain bounded", System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3));
                assertArrayEquals(snapshot.getSourceBytes(), Files.readAllBytes(path));
                assertEquals(1, ((Number) config.get("a")).intValue());
            }
        }
    }

    @Test public void externalEditDuringSharingRetryWinsOverStagedCandidate() throws Exception {
        Path path = file();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch writing = new CountDownLatch(1);
        try (ForgeConfigFile config = config(path, writing, false)) {
            config.load();
            ConfigEditSnapshot snapshot = config.snapshotForEdit(Collections.singleton("a"));
            try (FileChannel ignored = lease(path)) {
                Future<ConfigCommitResult> result = worker.submit(() -> config.compareAndSetAll(snapshot, Collections.singletonMap("a", 2)));
                assertTrue(writing.await(5, TimeUnit.SECONDS));
                Thread.sleep(75);
                assertFalse("Wait for the actual sharing lease before changing the original", result.isDone());
                Files.write(path, "a = 9\n".getBytes(StandardCharsets.UTF_8));
                assertEquals(ConfigCommitResult.CONFLICT, result.get(5, TimeUnit.SECONDS));
                assertEquals("a = 9\n", text(path));
                assertEquals(1, ((Number) config.get("a")).intValue());
            }
        } finally { stop(worker); }
    }

    @Test public void interruptedSharingWaitPreservesInterruptAndOriginal() throws Exception {
        Path path = file();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch writing = new CountDownLatch(1);
        AtomicBoolean interruptPreserved = new AtomicBoolean();
        java.util.concurrent.atomic.AtomicReference<Thread> thread = new java.util.concurrent.atomic.AtomicReference<>();
        try (ForgeConfigFile config = config(path, writing, false)) {
            config.load();
            ConfigEditSnapshot snapshot = config.snapshotForEdit(Collections.singleton("a"));
            try (FileChannel ignored = lease(path)) {
                Future<?> result = worker.submit(() -> {
                    thread.set(Thread.currentThread());
                    try { config.compareAndSetAll(snapshot, Collections.singletonMap("a", 2)); }
                    finally { interruptPreserved.set(Thread.currentThread().isInterrupted()); }
                });
                assertTrue(writing.await(5, TimeUnit.SECONDS));
                Thread.sleep(75);
                assertFalse(result.isDone());
                thread.get().interrupt();
                assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
                assertTrue(interruptPreserved.get());
                assertArrayEquals(snapshot.getSourceBytes(), Files.readAllBytes(path));
                assertEquals(1, ((Number) config.get("a")).intValue());
            }
        } finally { stop(worker); }
    }

    private Path file() throws Exception {
        Path path = temporary.newFile("config.toml").toPath();
        Files.write(path, "a = 1\n".getBytes(StandardCharsets.UTF_8));
        return path;
    }
    private static String text(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    private static ForgeConfigFile config(Path path, CountDownLatch writing, boolean correct) {
        return new ForgeConfigFile(CommentedFileConfig.of(path), new ForgeConfigSpec.Builder().build(),
                candidate -> { if (correct) candidate.set("a", 2); }, (candidate, output) -> {
                    com.electronwill.nightconfig.toml.TomlFormat.instance().createWriter().write(candidate, output);
                    writing.countDown();
                });
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static FileChannel lease(Path path) throws Exception {
        Assume.assumeTrue(System.getProperty("os.name", "").startsWith("Windows"));
        OpenOption denyDelete = (OpenOption) Enum.valueOf((Class) Class.forName("com.sun.nio.file.ExtendedOpenOption"), "NOSHARE_DELETE");
        return FileChannel.open(path, StandardOpenOption.READ, denyDelete);
    }
    private static void stop(ExecutorService worker) throws Exception {
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }
}
