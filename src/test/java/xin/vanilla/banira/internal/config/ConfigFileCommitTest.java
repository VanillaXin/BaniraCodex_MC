package xin.vanilla.banira.internal.config;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.InterruptedIOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class ConfigFileCommitTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void existingOriginalIsReplacedAtomically() throws Exception {
        Path target = file("config.toml", "original"), staged = file("staged.tmp", "candidate");
        assertTrue(ConfigFileCommit.replaceIfUnchanged(staged, target, bytes("original")));
        assertArrayEquals(bytes("candidate"), Files.readAllBytes(target));
        assertFalse(Files.exists(staged));
    }
    @Test public void missingOriginalCanBeCreated() throws Exception {
        Path target = temporary.getRoot().toPath().resolve("config.toml"), staged = file("staged.tmp", "candidate");
        assertTrue(ConfigFileCommit.replaceIfUnchanged(staged, target, null));
        assertArrayEquals(bytes("candidate"), Files.readAllBytes(target));
    }
    @Test public void changedOriginalRejectsCandidateWithoutMovingIt() throws Exception {
        Path target = file("config.toml", "external"), staged = file("staged.tmp", "candidate");
        assertFalse(ConfigFileCommit.replaceIfUnchanged(staged, target, bytes("old")));
        assertArrayEquals(bytes("external"), Files.readAllBytes(target));
        assertArrayEquals(bytes("candidate"), Files.readAllBytes(staged));
    }
    @Test public void windowsSharingLeaseCanExpireBeforeBoundedCommit() throws Exception {
        Path target = file("config.toml", "original"), staged = file("staged.tmp", "candidate");
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        try (FileChannel held = lease(target)) {
            Future<Boolean> result = worker.submit(() -> { entered.countDown(); return ConfigFileCommit.replaceIfUnchanged(staged, target, bytes("original")); });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Thread.sleep(75);
            assertFalse(result.isDone());
            held.close();
            assertTrue(result.get(5, TimeUnit.SECONDS));
            assertArrayEquals(bytes("candidate"), Files.readAllBytes(target));
        } finally { stop(worker); }
    }
    @Test public void persistentWindowsSharingDenialIsBoundedAndKeepsBothFiles() throws Exception {
        Path target = file("config.toml", "original"), staged = file("staged.tmp", "candidate");
        try (FileChannel ignored = lease(target)) {
            long start = System.nanoTime();
            assertThrows(AccessDeniedException.class, () -> ConfigFileCommit.replaceIfUnchanged(staged, target, bytes("original")));
            assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(3));
            assertArrayEquals(bytes("original"), Files.readAllBytes(target));
            assertArrayEquals(bytes("candidate"), Files.readAllBytes(staged));
        }
    }
    @Test public void interruptionBeforeCommitKeepsFilesAndInterruptFlag() throws Exception {
        Path target = file("config.toml", "original"), staged = file("staged.tmp", "candidate");
        ExecutorService worker = Executors.newSingleThreadExecutor();
        AtomicBoolean preserved = new AtomicBoolean();
        try {
            Future<?> result = worker.submit(() -> {
                Thread.currentThread().interrupt();
                try { assertThrows(InterruptedIOException.class, () -> ConfigFileCommit.replaceIfUnchanged(staged, target, bytes("original"))); }
                finally { preserved.set(Thread.currentThread().isInterrupted()); }
            });
            result.get(5, TimeUnit.SECONDS);
            assertTrue(preserved.get());
            assertArrayEquals(bytes("original"), Files.readAllBytes(target));
            assertArrayEquals(bytes("candidate"), Files.readAllBytes(staged));
        } finally { stop(worker); }
    }
    private Path file(String name, String content) throws Exception {
        Path path = temporary.newFile(name).toPath(); Files.write(path, bytes(content)); return path;
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static FileChannel lease(Path path) throws Exception {
        Assume.assumeTrue(System.getProperty("os.name", "").startsWith("Windows"));
        OpenOption denyDelete = (OpenOption) Enum.valueOf((Class) Class.forName("com.sun.nio.file.ExtendedOpenOption"), "NOSHARE_DELETE");
        return FileChannel.open(path, StandardOpenOption.READ, denyDelete);
    }
    private static void stop(ExecutorService worker) throws Exception {
        worker.shutdownNow(); assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }
}
