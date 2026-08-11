package xin.vanilla.banira.internal.neoforge.network;

import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;

import java.util.function.BiConsumer;
import java.util.function.Function;

/** 每个 Banira channel 独立维护协议和 packet id 的 NeoForge 注册器。 */
public final class NeoForgeNetworkHandler implements NetworkPacketRegistrar {
    private static final String DEFAULT_PROTOCOL = "1";

    private final ResourceLocation channel;
    private final String protocolVersion;
    private final boolean optionalClient;

    private NeoForgeNetworkHandler(ResourceLocation channel, String protocolVersion, boolean optionalClient) {
        this.channel = channel;
        this.protocolVersion = protocolVersion == null || protocolVersion.isBlank() ? DEFAULT_PROTOCOL : protocolVersion;
        this.optionalClient = optionalClient;
    }

    public static NeoForgeNetworkHandler create(String channelName, BaniraIdentifier identifier) {
        return create(channelName, identifier, DEFAULT_PROTOCOL, true);
    }

    public static NeoForgeNetworkHandler create(String channelName, BaniraIdentifier identifier,
                                                  String protocolVersion, boolean optionalClient) {
        return new NeoForgeNetworkHandler(ResourceLocation.fromNamespaceAndPath(
                identifier.getNamespace(), channelName), protocolVersion, optionalClient);
    }

    @Override
    public <MSG extends INetworkPacket> void register(int packetId,
                                                      Class<MSG> packetClass,
                                                      BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                      Function<BaniraPacketBuffer, MSG> decoder,
                                                      BiConsumer<MSG, BaniraNetworkContext> handler) {
        NeoForgeNetworkChannels.bind(channel, protocolVersion, optionalClient,
                packetId, packetClass, encoder, decoder, handler);
    }
}
