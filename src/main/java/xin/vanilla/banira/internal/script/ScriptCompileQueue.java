package xin.vanilla.banira.internal.script;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

/** Serial compiler queue, bounded by waiting owners rather than file count. */
final class ScriptCompileQueue {
    private static final Logger LOG = Logger.getLogger(ScriptCompileQueue.class.getName());
    static final ScriptCompileQueue SHARED = new ScriptCompileQueue(Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "banira-script-compiler");
        thread.setDaemon(true);
        thread.setContextClassLoader(ScriptCompileQueue.class.getClassLoader());
        return thread;
    }), 16);
    private final Executor executor;
    private final int capacity;
    private final LinkedHashMap<Object, Job> pending = new LinkedHashMap<>();
    private boolean running;

    ScriptCompileQueue(Executor executor, int capacity) {
        this.executor = executor;
        this.capacity = capacity;
    }

    void submit(Object key, Runnable task, Runnable cancelled) {
        Job old;
        synchronized (this) {
            if (!pending.containsKey(key) && pending.size() >= capacity) {
                throw new RejectedExecutionException("Script compile queue is full");
            }
            old = pending.put(key, new Job(task, cancelled));
            if (!running) {
                running = true;
                try { executor.execute(this::drain); }
                catch (RuntimeException error) {
                    pending.remove(key);
                    running = false;
                    throw error;
                }
            }
        }
        if (old != null) old.cancelled.run();
    }

    void cancel(Object key) {
        Job job;
        synchronized (this) { job = pending.remove(key); }
        if (job != null) job.cancelled.run();
    }

    private void drain() {
        while (true) {
            Job job;
            synchronized (this) {
                if (pending.isEmpty()) { running = false; return; }
                Iterator<Job> iterator = pending.values().iterator();
                job = iterator.next();
                iterator.remove();
            }
            try { job.task.run(); }
            catch (RuntimeException error) { LOG.log(Level.WARNING, "Script compilation task failed", error); }
            catch (Error error) {
                synchronized (this) { running = false; }
                throw error;
            }
        }
    }
    private static final class Job {
        final Runnable task;
        final Runnable cancelled;
        Job(Runnable task, Runnable cancelled) { this.task = task; this.cancelled = cancelled; }
    }
}
