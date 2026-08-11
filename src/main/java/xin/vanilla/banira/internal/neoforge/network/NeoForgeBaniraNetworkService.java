package xin.vanilla.banira.internal.neoforge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.platform.BaniraNetworkService;

import javax.annotation.Nonnull;

/** NeoForge 的公共网络服务实现。 */
public final class NeoForgeBaniraNetworkService implements BaniraNetworkService {
    public static final NeoForgeBaniraNetworkService INSTANCE = new NeoForgeBaniraNetworkService();

    private NeoForgeBaniraNetworkService() {
    }

    @Nonnull
    @Override
    public NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier) {
        return NeoForgeNetworkHandler.create(channelName, identifier);
    }

    @Nonnull
    @Override
    public NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier,
                                            @Nonnull String protocolVersion, boolean optionalClient) {
        return NeoForgeNetworkHandler.create(channelName, identifier, protocolVersion, optionalClient);
    }

    @Override public void sendToServer(@Nonnull BaniraNetworkPacket packet) { NeoForgeNetworkChannels.sendToServer(packet); }

    @Override
    public void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NeoForgeNetworkChannels.sendToPlayer(packet, serverPlayer);
        }
    }

    @Override public boolean hasDefaultChannel() { return NeoForgeNetworkChannels.hasDefaultChannel(); }

    @Override
    public boolean hasLocalChannel(@Nonnull String channelId) {
        ResourceLocation id = ResourceLocation.tryParse(channelId);
        return id != null && NeoForgeNetworkChannels.hasLocalChannel(id);
    }

    @Override
    public boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
        ResourceLocation id = ResourceLocation.tryParse(channelId);
        return player instanceof ServerPlayer serverPlayer && id != null
                && NeoForgeNetworkChannels.hasPlayerChannel(serverPlayer, id);
    }

    @Override
    public boolean isRemoteClientModInstalled(@Nonnull Object player, @Nonnull String modId) {
        return player instanceof ServerPlayer serverPlayer
                && PlayerUtils.isRemoteClientModInstalled(serverPlayer, modId);
    }
}
