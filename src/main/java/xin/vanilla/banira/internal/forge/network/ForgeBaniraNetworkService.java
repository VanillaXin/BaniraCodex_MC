package xin.vanilla.banira.internal.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
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
    public NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier) {
        return ForgeNetworkHandler.create(channelName, identifier);
    }

    @Nonnull
    @Override
    public NetworkPacketRegistrar registrar(@Nonnull String channelName,
                                            @Nonnull BaniraIdentifier identifier,
                                            @Nonnull String protocolVersion,
                                            boolean optionalClient) {
        return ForgeNetworkHandler.create(channelName, identifier, protocolVersion, optionalClient);
    }

    @Override
    public void sendToServer(@Nonnull BaniraNetworkPacket packet) {
        ForgeNetworkChannels.sendToServer(packet);
    }

    @Override
    public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
        if (player instanceof ServerPlayer) {
            ForgeNetworkChannels.sendToPlayer(packet, (ServerPlayer) player);
        }
    }

    @Override
    public boolean hasDefaultChannel() {
        return ForgeNetworkChannels.hasDefaultChannel();
    }

    @Override
    public boolean hasLocalChannel(@Nonnull String channelId) {
        ResourceLocation channel = ResourceLocation.tryParse(channelId);
        return channel != null && ForgeNetworkChannels.hasLocalChannel(channel);
    }

    @Override
    public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
        ResourceLocation channel = ResourceLocation.tryParse(channelId);
        return player instanceof ServerPlayer && channel != null
                && ForgeNetworkChannels.hasPlayerChannel((ServerPlayer) player, channel);
    }

    @Override
    public boolean isRemoteClientModInstalled(@Nonnull Object player, @Nonnull String modId) {
        return player instanceof ServerPlayer
                && PlayerUtils.isRemoteClientModInstalled((ServerPlayer) player, modId);
    }
}
