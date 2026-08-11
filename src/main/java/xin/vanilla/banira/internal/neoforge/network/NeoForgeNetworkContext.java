package xin.vanilla.banira.internal.neoforge.network;

import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xin.vanilla.banira.common.network.BaniraNetworkContext;

import javax.annotation.Nullable;

/** NeoForge payload 上下文的最小公共视图。 */
final class NeoForgeNetworkContext implements BaniraNetworkContext {
    private final IPayloadContext delegate;

    NeoForgeNetworkContext(IPayloadContext delegate) {
        this.delegate = delegate;
    }

    @Override public void enqueueWork(Runnable work) { delegate.enqueueWork(work); }
    @Override public void markHandled() { }
    @Override public boolean isClientSide() { return delegate.flow() == PacketFlow.CLIENTBOUND; }
    @Override public boolean isServerSide() { return delegate.flow() == PacketFlow.SERVERBOUND; }
    @Nullable @Override public Object sender() { return delegate.player(); }
}
