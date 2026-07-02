package xin.vanilla.banira.internal.fabric.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.platform.BaniraNetworkService;

import javax.annotation.Nonnull;

/**
 * Fabric 分支的 Banira 网络服务。
 */
public enum FabricBaniraNetworkService implements BaniraNetworkService {
    INSTANCE;

    @Nonnull
    @Override
    public NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier) {
        return FabricNetworkHandler.create(channelName, identifier);
    }

    @Override
    public void sendToServer(@Nonnull BaniraNetworkPacket packet) {
        FabricNetworkChannels.sendToServer(packet);
    }

    @Override
    public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
        if (player instanceof ServerPlayer serverPlayer) {
            FabricNetworkChannels.sendToPlayer(packet, serverPlayer);
        }
    }

    @Override
    public boolean hasDefaultChannel() {
        return FabricNetworkChannels.hasDefaultChannel();
    }

    @Override
    public boolean hasLocalChannel(@Nonnull String channelId) {
        ResourceLocation channel = ResourceLocation.tryParse(channelId);
        return channel != null && FabricNetworkChannels.hasLocalChannel(channel);
    }

    @Override
    public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
        ResourceLocation channel = ResourceLocation.tryParse(channelId);
        return player instanceof ServerPlayer && channel != null
                && FabricNetworkChannels.hasPlayerChannel((ServerPlayer) player, channel);
    }
}
