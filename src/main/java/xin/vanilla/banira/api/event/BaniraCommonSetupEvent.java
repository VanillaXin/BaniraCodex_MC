package xin.vanilla.banira.api.event;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 公共初始化阶段事件；加载器自己的 setup 事件只在适配层转换成此类型。
 */
public final class BaniraCommonSetupEvent {
    private final Consumer<Runnable> workQueue;

    private BaniraCommonSetupEvent(Consumer<Runnable> workQueue) {
        this.workQueue = workQueue;
    }

    public static BaniraCommonSetupEvent immediate() {
        return new BaniraCommonSetupEvent(Runnable::run);
    }

    public static BaniraCommonSetupEvent withWorkQueue(@Nonnull Consumer<Runnable> workQueue) {
        return new BaniraCommonSetupEvent(Objects.requireNonNull(workQueue, "workQueue"));
    }

    /**
     * 在加载器允许的主线程/安全阶段执行工作，Forge 下会桥接到 FMLCommonSetupEvent.enqueueWork。
     */
    public void enqueueWork(@Nonnull Runnable task) {
        workQueue.accept(Objects.requireNonNull(task, "task"));
    }
}
