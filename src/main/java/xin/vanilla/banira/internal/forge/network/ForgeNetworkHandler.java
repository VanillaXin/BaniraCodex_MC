package xin.vanilla.banira.internal.forge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Forge 的 SimpleChannel 注册实现；公共 NetworkHandler 不直接暴露该类型。
 */
public final class ForgeNetworkHandler implements NetworkPacketRegistrar {
    private static final String PROTOCOL_VERSION = "1";

    private final SimpleChannel channel;

    private ForgeNetworkHandler(SimpleChannel channel) {
        this.channel = channel;
    }

    public static ForgeNetworkHandler create(String channelName, BaniraIdentifier identifier) {
        return create(channelName, identifier, PROTOCOL_VERSION, true);
    }

    public static ForgeNetworkHandler create(String channelName, BaniraIdentifier identifier,
                                              String protocolVersion, boolean optionalClient) {
        int networkVersion = networkVersion(protocolVersion);
        ChannelBuilder builder = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(
                identifier.getNamespace(), channelName)).networkProtocolVersion(networkVersion);
        if (optionalClient) {
            builder.optional();
        }
        SimpleChannel channel = builder.simpleChannel();
        ForgeNetworkChannels.installDefault(channel);
        return new ForgeNetworkHandler(channel);
    }

    private static int networkVersion(String protocolVersion) {
        try {
            return Integer.parseInt(protocolVersion);
        } catch (NumberFormatException ignored) {
            return protocolVersion.hashCode() & Integer.MAX_VALUE;
        }
    }

    @Override
    public <MSG extends INetworkPacket> void register(int packetId,
                                                      Class<MSG> packetClass,
                                                      BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                      Function<BaniraPacketBuffer, MSG> decoder,
                                                      BiConsumer<MSG, BaniraNetworkContext> handler) {
        // 子 mod 可拥有独立 channel，发送时必须按包类型找回注册通道。
        ForgeNetworkChannels.bind(packetClass, channel);
        channel.messageBuilder(packetClass, packetId)
                .encoder((packet, buffer) -> encoder.accept(packet, new ForgePacketBuffer(buffer)))
                .decoder(buffer -> decoder.apply(new ForgePacketBuffer(buffer)))
                .consumer((packet, context) -> handler.accept(packet, new ForgeNetworkContext(context)))
                .add();
    }
}
