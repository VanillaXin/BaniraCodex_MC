package xin.vanilla.banira.internal.forge.network;

import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.banira.common.network.BaniraNetworkContext;

import javax.annotation.Nullable;

/**
 * Forge 的 NetworkEvent.Context 到 Banira 上下文的薄封装。
 */
public final class ForgeNetworkContext implements BaniraNetworkContext {
    private final CustomPayloadEvent.Context context;

    public ForgeNetworkContext(CustomPayloadEvent.Context context) {
        this.context = context;
    }

    @Override
    public void enqueueWork(Runnable work) {
        context.enqueueWork(work);
    }

    @Override
    public void markHandled() {
        context.setPacketHandled(true);
    }

    @Override
    public boolean isClientSide() {
        return context.isClientSide();
    }

    @Override
    public boolean isServerSide() {
        return context.isServerSide();
    }

    @Nullable
    @Override
    public Object sender() {
        return context.getSender();
    }
}
