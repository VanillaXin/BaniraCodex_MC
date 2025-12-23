package xin.vanilla.banira.common.util;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.ScheduledTask;

import javax.annotation.Nonnull;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public final class BaniraScheduler {
    private BaniraScheduler() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final PriorityBlockingQueue<ScheduledTask> serverTasks = new PriorityBlockingQueue<>();
    private static final PriorityBlockingQueue<ScheduledTask> clientTasks = new PriorityBlockingQueue<>();


    private static final AtomicLong serverExecutedCount = new AtomicLong(0);
    private static final AtomicLong clientExecutedCount = new AtomicLong(0);
    private static final AtomicLong clientTicks = new AtomicLong(0);


    public static void schedule(@Nonnull MinecraftServer server, int delayTicks, @Nonnull Runnable action) {
        long executeAt = server.getTickCount() + Math.max(0, delayTicks);
        serverTasks.add(ScheduledTask.server(executeAt, action));
    }

    @OnlyIn(Dist.CLIENT)
    public static void schedule(int delayTicks, @Nonnull Runnable action) {
        long executeAt = clientTicks.get() + Math.max(0, delayTicks);
        clientTasks.add(ScheduledTask.client(executeAt, action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = BaniraCodex.serverInstance().key();
        if (server == null) return;

        runTask(server.getTickCount(), serverTasks, serverExecutedCount);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        runTask(clientTicks.incrementAndGet(), clientTasks, clientExecutedCount);
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

    public static int getServerPendingTaskCount() {
        return serverTasks.size();
    }

    public static long getServerExecutedCount() {
        return serverExecutedCount.get();
    }

    public static int getClientPendingTaskCount() {
        return clientTasks.size();
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

}
