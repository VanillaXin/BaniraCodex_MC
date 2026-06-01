package xin.vanilla.banira.common.network;

import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nullable;

/**
 * Loader-neutral packet handling context.
 */
public final class BaniraNetworkContext {
    private final BaniraNetworkContextDelegate delegate;

    public BaniraNetworkContext(BaniraNetworkContextDelegate delegate) {
        this.delegate = delegate;
    }

    public void enqueueWork(Runnable runnable) {
        delegate.enqueueWork(runnable);
    }

    @Nullable
    public ServerPlayerEntity sender() {
        return delegate.sender();
    }

    public boolean isClientReception() {
        return delegate.isClientReception();
    }

    public boolean isServerReception() {
        return delegate.isServerReception();
    }

    public void markHandled() {
        delegate.markHandled();
    }
}
