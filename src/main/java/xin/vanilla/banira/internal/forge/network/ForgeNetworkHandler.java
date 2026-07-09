package xin.vanilla.banira.internal.forge.network;

import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
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
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(identifier.getNamespace(), channelName),
                () -> PROTOCOL_VERSION,
                clientVersion -> true,
                serverVersion -> true
        );
        ForgeNetworkChannels.installDefault(channel);
        return new ForgeNetworkHandler(channel);
    }

    @Override
    public <MSG extends INetworkPacket> void register(int packetId,
                                                      Class<MSG> packetClass,
                                                      BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                      Function<BaniraPacketBuffer, MSG> decoder,
                                                      BiConsumer<MSG, BaniraNetworkContext> handler) {
        // 子 mod 可拥有独立 channel，发送时必须按包类型找回注册通道。
        ForgeNetworkChannels.bind(packetClass, channel);
        channel.registerMessage(
                packetId,
                packetClass,
                (packet, buffer) -> encoder.accept(packet, new ForgePacketBuffer(buffer)),
                buffer -> decoder.apply(new ForgePacketBuffer(buffer)),
                (packet, ctx) -> handler.accept(packet, new ForgeNetworkContext(ctx))
        );
    }
}
