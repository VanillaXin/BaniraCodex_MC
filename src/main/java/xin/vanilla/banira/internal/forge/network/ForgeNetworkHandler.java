package xin.vanilla.banira.internal.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.IIdentifier;

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

    public static ForgeNetworkHandler create(String channelName, IIdentifier identifier) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                identifier.create(channelName),
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
                                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                                      Function<FriendlyByteBuf, MSG> decoder,
                                                      BiConsumer<MSG, BaniraNetworkContext> handler) {
        channel.registerMessage(
                packetId,
                packetClass,
                encoder,
                decoder,
                (packet, ctx) -> handler.accept(packet, new ForgeNetworkContext(ctx))
        );
    }
}
