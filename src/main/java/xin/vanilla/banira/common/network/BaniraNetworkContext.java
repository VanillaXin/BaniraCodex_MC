package xin.vanilla.banira.common.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Loader-neutral packet handling context.
 * <p>
 * Forge-specific access is kept available for this branch only as a migration
 * escape hatch.
 */
public final class BaniraNetworkContext {
    private final Supplier<NetworkEvent.Context> forgeContext;

    private BaniraNetworkContext(Supplier<NetworkEvent.Context> forgeContext) {
        this.forgeContext = forgeContext;
    }

    public static BaniraNetworkContext forge(Supplier<NetworkEvent.Context> forgeContext) {
        return new BaniraNetworkContext(forgeContext);
    }

    public void enqueueWork(Runnable runnable) {
        forgeContext.get().enqueueWork(runnable);
    }

    @Nullable
    public ServerPlayerEntity sender() {
        return forgeContext.get().getSender();
    }

    public void markHandled() {
        forgeContext.get().setPacketHandled(true);
    }

    /**
     * @deprecated Loader-specific access; avoid in dependent mods.
     */
    @Deprecated
    public NetworkEvent.Context forgeContext() {
        return forgeContext.get();
    }
}
