package xin.vanilla.banira.internal.config;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final class ConfigFileCommit {
    private static final long SHARING_RETRY_NANOS = TimeUnit.SECONDS.toNanos(1);

    private ConfigFileCommit() {}

    public static boolean replaceIfUnchanged(Path staged, Path target, byte[] expected) throws IOException {
        long started = System.nanoTime();
        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Config commit interrupted: " + target);
            byte[] actual = Files.exists(target) ? Files.readAllBytes(target) : null;
            if (!Arrays.equals(expected, actual)) return false;
            try {
                Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return true;
            } catch (AccessDeniedException denied) {
                if (!System.getProperty("os.name", "").startsWith("Windows")
                        || System.nanoTime() - started >= SHARING_RETRY_NANOS) throw denied;
                // Recheck the original after every sharing delay; never fall back to a non-atomic write.
                try { Thread.sleep(25); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    InterruptedIOException failure = new InterruptedIOException("Config commit interrupted: " + target);
                    failure.initCause(interrupted);
                    failure.addSuppressed(denied);
                    throw failure;
                }
            }
        }
    }
}
