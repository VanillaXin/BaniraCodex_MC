package xin.vanilla.banira.internal.forge.config;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Supplements legacy NightConfig's skipped/coalesced events without replacing its registrations.
 */
public final class ForgeConfigWatch implements AutoCloseable {
    private static final ForgeConfigWatch INSTANCE = new ForgeConfigWatch(
            error -> org.apache.logging.log4j.LogManager.getLogger().warn("Cannot reload externally edited config", error));
    private final Map<Path, Runnable> callbacks = new HashMap<>();
    private final Map<Path, WatchKey> directories = new HashMap<>();
    private final Consumer<RuntimeException> errors;
    private WatchService service;

    ForgeConfigWatch(Consumer<RuntimeException> errors) {
        this.errors = errors;
    }

    public static void add(Path path, Runnable callback) throws IOException {
        INSTANCE.watch(path, callback);
    }

    public static void remove(Path path) {
        INSTANCE.unwatch(path);
    }

    synchronized void watch(Path path, Runnable callback) throws IOException {
        path = path.toAbsolutePath().normalize();
        if (service == null) {
            WatchService created = path.getFileSystem().newWatchService();
            service = created;
            Thread worker = new Thread(() -> run(created), "Banira config events");
            worker.setDaemon(true);
            worker.start();
        }
        Path parent = path.getParent();
        if (!directories.containsKey(parent)) {
            directories.put(parent, parent.register(service, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE));
        }
        callbacks.put(path, callback);
    }

    synchronized void unwatch(Path path) {
        path = path.toAbsolutePath().normalize();
        callbacks.remove(path);
        Path parent = path.getParent();
        if (callbacks.keySet().stream().noneMatch(file -> file.getParent().equals(parent))) {
            WatchKey key = directories.remove(parent);
            if (key != null) key.cancel();
        }
        if (callbacks.isEmpty()) close();
    }

    private void run(WatchService active) {
        try {
            while (true) {
                Set<Path> changed = new LinkedHashSet<>();
                collect(active.take(), changed);
                // A bounded batch allows truncate/write and atomic replacement to settle without per-frame IO.
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(50);
                WatchKey extra;
                long remaining;
                while ((remaining = deadline - System.nanoTime()) > 0
                        && (extra = active.poll(remaining, TimeUnit.NANOSECONDS)) != null) collect(extra, changed);
                for (Path path : changed) {
                    Runnable callback;
                    synchronized (this) {
                        callback = service == active ? callbacks.get(path) : null;
                    }
                    if (callback != null && Files.isRegularFile(path)) {
                        try {
                            callback.run();
                        } catch (RuntimeException error) {
                            errors.accept(error);
                        }
                    }
                }
            }
        } catch (ClosedWatchServiceException ignored) {
            // Last registration was unloaded.
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void collect(WatchKey key, Set<Path> changed) {
        Path directory = (Path) key.watchable();
        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                synchronized (this) {
                    for (Path path : callbacks.keySet()) if (path.getParent().equals(directory)) changed.add(path);
                }
            } else if (event.context() instanceof Path) changed.add(directory.resolve((Path) event.context()));
        }
        key.reset();
    }

    @Override
    public synchronized void close() {
        callbacks.clear();
        directories.clear();
        if (service != null) {
            try {
                service.close();
            } catch (IOException error) {
                errors.accept(new IllegalStateException("Cannot close config watcher", error));
            }
            service = null;
        }
    }
}
