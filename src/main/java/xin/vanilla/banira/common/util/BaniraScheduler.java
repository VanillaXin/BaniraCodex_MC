package xin.vanilla.banira.common.util;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.ScheduledTask;
import xin.vanilla.banira.common.data.WallClockScheduledTask;
import xin.vanilla.banira.internal.server.BaniraServerAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nonnull;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class BaniraScheduler {
    private BaniraScheduler() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final PriorityBlockingQueue<ScheduledTask> serverTasks = new PriorityBlockingQueue<>();
    private static final PriorityBlockingQueue<ScheduledTask> clientTasks = new PriorityBlockingQueue<>();
    private static final PriorityBlockingQueue<WallClockScheduledTask> serverWallClockTasks = new PriorityBlockingQueue<>();
    private static final PriorityBlockingQueue<WallClockScheduledTask> clientWallClockTasks = new PriorityBlockingQueue<>();


    private static final AtomicLong serverExecutedCount = new AtomicLong(0);
    private static final AtomicLong clientExecutedCount = new AtomicLong(0);
    private static final AtomicLong clientTicks = new AtomicLong(0);

    private static volatile boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        BaniraEventBus.Server.onTickEnd(BaniraScheduler::onServerTickEnd);
        BaniraEventBus.Client.onTickEnd(BaniraScheduler::onClientTickEnd);
    }

    /**
     * 服务端刻延迟任务；服务器刻来源由当前分支的 platform adapter 提供。
     */
    public static void scheduleServer(int delayTicks, @Nonnull Runnable action) {
        long executeAt = currentServerTick() + Math.max(0, delayTicks);
        serverTasks.add(ScheduledTask.server(executeAt, action));
    }

    /**
     * 兼容旧调用形状，实际不再读取 MinecraftServer#getTickCount。
     */
    public static void schedule(@Nonnull MinecraftServer server, int delayTicks, @Nonnull Runnable action) {
        scheduleServer(delayTicks, action);
    }

    public static void schedule(int delayTicks, @Nonnull Runnable action) {
        long executeAt = clientTicks.get() + Math.max(0, delayTicks);
        clientTasks.add(ScheduledTask.client(executeAt, action));
    }

    /**
     * 在墙钟时间经过 {@code delayMillis} 毫秒后执行，
     * 仍在本 mod 的服务器刻末派发，刻停止时任务不会执行，直至恢复刻循环。
     *
     * @param server      用于保持与 {@link #schedule(MinecraftServer, int, Runnable)} 一致的调用方式
     * @param delayMillis 延迟毫秒数，可为小数；非正数表示下一服务器刻末尽快执行
     */
    public static void scheduleAfterMillis(@Nonnull MinecraftServer server, double delayMillis, @Nonnull Runnable action) {
        serverWallClockTasks.add(WallClockScheduledTask.server(delayMillis, action));
    }

    /**
     * 客户端：墙钟延迟 {@code delayMillis} 毫秒
     */
    public static void scheduleAfterMillis(double delayMillis, @Nonnull Runnable action) {
        clientWallClockTasks.add(WallClockScheduledTask.client(delayMillis, action));
    }

    private static void onServerTickEnd() {
        long tick = currentServerTick();
        if (tick <= 0) return;
        runTask(tick, serverTasks, serverExecutedCount);
        runWallClockTask(serverWallClockTasks, serverExecutedCount);
    }

    private static void onClientTickEnd() {
        long tick = clientTicks.incrementAndGet();
        runTask(tick, clientTasks, clientExecutedCount);
        runWallClockTask(clientWallClockTasks, clientExecutedCount);
    }

    private static void runTask(long currentTick, PriorityBlockingQueue<ScheduledTask> scheduledTasks, AtomicLong executedCount) {
        try {
            while (true) {
                ScheduledTask task = scheduledTasks.peek();
                if (task == null) break;
                if (task.executeTick() <= currentTick) {
                    task = scheduledTasks.poll();
                    if (task == null) break;
                    try {
                        task.runnable().run();
                        executedCount.incrementAndGet();
                    } catch (Throwable t) {
                        LOGGER.warn("Scheduled task threw an exception", t);
                    }
                } else {
                    break;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed while executing scheduled tasks", t);
        }
    }

    private static void runWallClockTask(PriorityBlockingQueue<WallClockScheduledTask> wallClockTasks, AtomicLong executedCount) {
        try {
            long now = System.nanoTime();
            while (true) {
                WallClockScheduledTask task = wallClockTasks.peek();
                if (task == null) break;
                if (task.executeDeadlineNanos() <= now) {
                    task = wallClockTasks.poll();
                    if (task == null) break;
                    try {
                        task.runnable().run();
                        executedCount.incrementAndGet();
                    } catch (Throwable t) {
                        LOGGER.warn("Wall-clock scheduled task threw an exception", t);
                    }
                } else {
                    break;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed while executing wall-clock scheduled tasks", t);
        }
    }

    private static long currentServerTick() {
        return BaniraPlatforms.isInstalled() ? BaniraServerAccess.tickCount() : 0;
    }

    public static int getServerPendingTaskCount() {
        return serverTasks.size() + serverWallClockTasks.size();
    }

    public static long getServerExecutedCount() {
        return serverExecutedCount.get();
    }

    public static int getClientPendingTaskCount() {
        return clientTasks.size() + clientWallClockTasks.size();
    }

    public static long getClientExecutedCount() {
        return clientExecutedCount.get();
    }

    public static int getPendingTaskCount() {
        return getServerPendingTaskCount() + getClientPendingTaskCount();
    }

    public static long getExecutedCount() {
        return getServerExecutedCount() + getClientExecutedCount();
    }

    public static boolean removeTask(ScheduledTask task) {
        if (task == null) return false;
        return serverTasks.remove(task);
    }

    public static boolean removeWallClockTask(WallClockScheduledTask task) {
        if (task == null) return false;
        return serverWallClockTasks.remove(task) || clientWallClockTasks.remove(task);
    }

}
