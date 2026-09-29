package xin.vanilla.banira.internal.client;

import org.apache.logging.log4j.LogManager;
import xin.vanilla.banira.client.data.NotificationLogEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Coalesces pending snapshots; all writes and exit flushing share one IO lock. */
final class NotificationLogWriter implements AutoCloseable {
    private final Path path;
    private final Object ioLock = new Object();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "BaniraCodex-NotificationLogSave");
        thread.setDaemon(true);
        return thread;
    });
    private List<NotificationLogEntry> pending;
    private boolean scheduled;
    private boolean closed;

    NotificationLogWriter(Path path) {
        this.path = path;
    }

    synchronized void submit(List<NotificationLogEntry> snapshot) {
        if (closed) throw new IllegalStateException("Notification log writer is closed");
        pending = new ArrayList<>(snapshot.size());
        for (NotificationLogEntry entry : snapshot) pending.add(entry.copy());
        if (!scheduled) {
            scheduled = true;
            executor.schedule(() -> {
                synchronized (this) { scheduled = false; }
                try {
                    flush();
                } catch (IOException error) {
                    LogManager.getLogger().warn("Failed to save notification log", error);
                }
            }, 100, TimeUnit.MILLISECONDS);
        }
    }

    void flush() throws IOException {
        synchronized (ioLock) {
            List<NotificationLogEntry> snapshot;
            synchronized (this) {
                snapshot = pending;
                pending = null;
            }
            if (snapshot == null) return;
            try {
                NotificationLogStore.save(path, snapshot);
            } catch (IOException error) {
                synchronized (this) {
                    if (pending == null) pending = snapshot;
                }
                throw error;
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) { closed = true; }
        executor.shutdown();
        flush();
    }
}
