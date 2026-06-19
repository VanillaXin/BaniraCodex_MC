package xin.vanilla.banira.internal.forge.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.common.network.BaniraNetworkContext;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Forge 的 NetworkEvent.Context 到 Banira 上下文的薄封装。
 */
final class ForgeNetworkContext implements BaniraNetworkContext {
    private final Supplier<NetworkEvent.Context> context;

    ForgeNetworkContext(Supplier<NetworkEvent.Context> context) {
        this.context = context;
    }

    @Override
    public void enqueueWork(Runnable work) {
        context.get().enqueueWork(work);
    }

    @Override
    public void markHandled() {
        context.get().setPacketHandled(true);
    }

    @Override
    public boolean isClientSide() {
        return context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT;
    }

    @Override
    public boolean isServerSide() {
        return context.get().getDirection().getReceptionSide() == LogicalSide.SERVER;
    }

    @Nullable
    @Override
    public ServerPlayerEntity sender() {
        return context.get().getSender();
    }
}
