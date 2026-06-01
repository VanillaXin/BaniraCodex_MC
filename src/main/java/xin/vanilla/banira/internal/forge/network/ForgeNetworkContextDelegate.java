package xin.vanilla.banira.internal.forge.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.banira.common.network.BaniraNetworkContextDelegate;

import javax.annotation.Nullable;
import java.util.function.Supplier;

final class ForgeNetworkContextDelegate implements BaniraNetworkContextDelegate {
    private final Supplier<NetworkEvent.Context> context;

    ForgeNetworkContextDelegate(Supplier<NetworkEvent.Context> context) {
        this.context = context;
    }

    @Override
    public void enqueueWork(Runnable runnable) {
        context.get().enqueueWork(runnable);
    }

    @Nullable
    @Override
    public ServerPlayerEntity sender() {
        return context.get().getSender();
    }

    @Override
    public boolean isClientReception() {
        return context.get().getDirection().getReceptionSide() == LogicalSide.CLIENT;
    }

    @Override
    public boolean isServerReception() {
        return context.get().getDirection().getReceptionSide() == LogicalSide.SERVER;
    }

    @Override
    public void markHandled() {
        context.get().setPacketHandled(true);
    }
}
