package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.forge.network.ForgeNetworkChannel;
import xin.vanilla.banira.internal.mixin.accessors.NetworkRegistryAccessor;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.platform.BaniraNetworkService;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Forge 1.16.5 网络实现；SimpleChannel 等加载器细节不进入公共 platform API。
 */
final class ForgeBaniraNetworkService implements BaniraNetworkService {
    private final Map<ResourceLocation, ForgeNetworkChannel> channels = new ConcurrentHashMap<>();
    private NetworkRegistryAccessor networkRegistry;

    @Override
    public NetworkPacketRegistrar registrar(String channelName, IIdentifier identifier) {
        ForgeNetworkChannel channel = create(channelName, identifier);
        return new NetworkPacketRegistrar() {
            @Override
            public <MSG extends INetworkPacket> void register(
                    int packetId,
                    Class<MSG> packetClass,
                    BiConsumer<MSG, BaniraPacketBuffer> encoder,
                    Function<BaniraPacketBuffer, MSG> decoder,
                    BiConsumer<MSG, BaniraNetworkContext> handler) {
                channel.register(packetId, packetClass, encoder, decoder, handler);
            }
        };
    }

    @Override
    public void sendToServer(BaniraNetworkPacket packet) {
        INetworkPacket networkPacket = asNetworkPacket(packet);
        ForgeNetworkChannel channel = channels.get(networkPacket.networkChannelName());
        if (channel == null || !hasLocalChannel(channel.channelName())) {
            return;
        }
        PlayerEntity player = BaniraPlatforms.get().client().localPlayer();
        if (player == null) {
            return;
        }
        if (!(packet instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(player, channel.modId())) {
            return;
        }
        channel.sendToServer(networkPacket);
    }

    @Override
    public void sendToPlayer(BaniraNetworkPacket packet, ServerPlayerEntity player) {
        INetworkPacket networkPacket = asNetworkPacket(packet);
        ForgeNetworkChannel channel = channels.get(networkPacket.networkChannelName());
        if (channel == null || !hasPlayerChannel(player, channel.channelName())) {
            return;
        }
        if (!PlayerUtils.isRemoteClientModInstalled(player, channel.modId())) {
            return;
        }
        channel.sendToPlayer(player, networkPacket);
    }

    @Override
    public boolean hasDefaultChannel() {
        return hasLocalChannel(NetworkInit.DEFAULT_CHANNEL_NAME);
    }

    @Override
    public boolean hasLocalChannel(ResourceLocation channel) {
        return channel != null && registry().banira$instances().containsKey(channel);
    }

    @Override
    public boolean hasPlayerChannel(ServerPlayerEntity player, ResourceLocation channel) {
        return hasLocalChannel(channel);
    }

    private ForgeNetworkChannel create(String channelName, IIdentifier identifier) {
        ForgeNetworkChannel channel = ForgeNetworkChannel.create(channelName, identifier);
        channels.put(channel.channelName(), channel);
        return channel;
    }

    private static INetworkPacket asNetworkPacket(BaniraNetworkPacket packet) {
        if (packet instanceof INetworkPacket) {
            return (INetworkPacket) packet;
        }
        throw new IllegalArgumentException("BaniraNetworkPacket must also implement INetworkPacket on Forge 1.16.5");
    }

    private NetworkRegistryAccessor registry() {
        if (networkRegistry == null) {
            networkRegistry = (NetworkRegistryAccessor) new NetworkRegistry();
        }
        return networkRegistry;
    }
}
