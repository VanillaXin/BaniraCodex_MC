package xin.vanilla.banira.client.util;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 合并高频异步任务：执行中再次请求时，只在当前任务结束后补跑一次最新状态。
 */
public final class CoalescingAsyncTask {

    private final Object lock = new Object();
    private final ExecutorService executor;
    private final Task task;
    private final Consumer<Throwable> errorHandler;

    private boolean running;
    private boolean rerunRequested;

    public CoalescingAsyncTask(String threadName, Task task, Consumer<Throwable> errorHandler) {
        this.task = Objects.requireNonNull(task, "task");
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    public void request() {
        synchronized (lock) {
            if (running) {
                rerunRequested = true;
                return;
            }
            running = true;
        }
        executor.execute(this::runLoop);
    }

    private void runLoop() {
        while (true) {
            try {
                task.run();
            } catch (Throwable t) {
                errorHandler.accept(t);
            }
            synchronized (lock) {
                if (rerunRequested) {
                    rerunRequested = false;
                    continue;
                }
                running = false;
                return;
            }
        }
    }

    @FunctionalInterface
    public interface Task {
        void run() throws Exception;
    }
}
