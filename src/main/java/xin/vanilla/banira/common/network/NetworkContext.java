package xin.vanilla.banira.common.network;

import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric 网络回调的最小上下文，替代 Forge 的 NetworkEvent.Context。
 */
public final class NetworkContext {
    private final boolean serverSide;
    private final ServerPlayer sender;
    private final RunnableExecutor executor;

    public NetworkContext(boolean serverSide, ServerPlayer sender, RunnableExecutor executor) {
        this.serverSide = serverSide;
        this.sender = sender;
        this.executor = executor;
    }

    public boolean isServerSide() {
        return serverSide;
    }

    public boolean isClientSide() {
        return !serverSide;
    }

    public ServerPlayer sender() {
        return sender;
    }

    public void enqueueWork(Runnable runnable) {
        executor.execute(runnable);
    }

    @FunctionalInterface
    public interface RunnableExecutor {
        void execute(Runnable runnable);
    }
}
