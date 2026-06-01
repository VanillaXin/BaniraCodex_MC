package xin.vanilla.banira.platform.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.util.IIdentifier;

/**
 * Loader-neutral network channel creation surface.
 */
public interface BaniraNetworkService {
    BaniraNetworkChannel create(String channelName, IIdentifier identifier);

    boolean hasChannel(ResourceLocation channelName);

    default boolean hasChannel(BaniraNetworkChannel channel) {
        return hasChannel(channel.channelName());
    }

    default boolean hasChannel(ServerPlayerEntity player, ResourceLocation channelName) {
        return hasChannel(channelName);
    }

    default boolean hasChannel(ServerPlayerEntity player, BaniraNetworkChannel channel) {
        return hasChannel(player, channel.channelName());
    }
}
