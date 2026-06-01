package xin.vanilla.banira.common.network;

import net.minecraft.entity.player.ServerPlayerEntity;

import javax.annotation.Nullable;

/**
 * Loader adapter for {@link BaniraNetworkContext}.
 */
public interface BaniraNetworkContextDelegate {
    void enqueueWork(Runnable runnable);

    @Nullable
    ServerPlayerEntity sender();

    boolean isClientReception();

    boolean isServerReception();

    void markHandled();
}
