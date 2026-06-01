package xin.vanilla.banira.common.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Loader-neutral packet handling context.
 */
public final class BaniraNetworkContext {
    private final Supplier<NetworkEvent.Context> forgeContext;

    private BaniraNetworkContext(Supplier<NetworkEvent.Context> forgeContext) {
        this.forgeContext = forgeContext;
    }

    static BaniraNetworkContext forge(Supplier<NetworkEvent.Context> forgeContext) {
        return new BaniraNetworkContext(forgeContext);
    }

    public void enqueueWork(Runnable runnable) {
        forgeContext.get().enqueueWork(runnable);
    }

    @Nullable
    public ServerPlayerEntity sender() {
        return forgeContext.get().getSender();
    }

    public boolean isClientReception() {
        return forgeContext.get().getDirection().getReceptionSide() == LogicalSide.CLIENT;
    }

    public boolean isServerReception() {
        return forgeContext.get().getDirection().getReceptionSide() == LogicalSide.SERVER;
    }

    public void markHandled() {
        forgeContext.get().setPacketHandled(true);
    }

}
