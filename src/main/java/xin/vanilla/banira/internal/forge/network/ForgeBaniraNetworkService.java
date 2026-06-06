package xin.vanilla.banira.internal.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.platform.BaniraNetworkService;

import javax.annotation.Nonnull;

/**
 * Forge 分支的 Banira 网络服务实现。
 */
public final class ForgeBaniraNetworkService implements BaniraNetworkService {
    public static final ForgeBaniraNetworkService INSTANCE = new ForgeBaniraNetworkService();

    private ForgeBaniraNetworkService() {
    }

    @Nonnull
    @Override
    public NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull IIdentifier identifier) {
        return ForgeNetworkHandler.create(channelName, identifier);
    }

    @Override
    public void sendToServer(@Nonnull INetworkPacket packet) {
        ForgeNetworkChannels.sendToServer(packet);
    }

    @Override
    public void sendToPlayer(@Nonnull INetworkPacket packet, @Nonnull ServerPlayer player) {
        ForgeNetworkChannels.sendToPlayer(packet, player);
    }

    @Override
    public boolean hasDefaultChannel() {
        return ForgeNetworkChannels.hasDefaultChannel();
    }

    @Override
    public boolean hasLocalChannel(@Nonnull ResourceLocation channel) {
        return ForgeNetworkChannels.hasLocalChannel(channel);
    }

    @Override
    public boolean hasPlayerChannel(@Nonnull ServerPlayer player, @Nonnull ResourceLocation channel) {
        return ForgeNetworkChannels.hasPlayerChannel(player, channel);
    }
}
