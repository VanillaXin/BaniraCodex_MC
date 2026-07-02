package xin.vanilla.banira.common.network;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Forge 1.20.1 network channel wrapper kept behind Banira's neutral API boundary.
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static int nextPacketId = 0;

    @Getter
    private final SimpleChannel channel;

    public static NetworkHandler create(String channelName, BaniraIdentifier identifier) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(identifier.getNamespace(), channelName),
                () -> PROTOCOL_VERSION,
                clientVersion -> true,
                serverVersion -> true
        );
        return new NetworkHandler(channel);
    }

    private NetworkHandler(SimpleChannel channel) {
        this.channel = channel;
    }

    public <MSG extends INetworkPacket> void register(Class<MSG> packetClass,
                                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                                      Function<FriendlyByteBuf, MSG> decoder,
                                                      BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        channel.registerMessage(
                nextPacketId++,
                packetClass,
                encoder,
                decoder,
                handler
        );
    }

    public <MSG extends SplitPacket & INetworkPacket> void registerSplit(
            Class<MSG> packetClass,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        BiConsumer<MSG, Supplier<NetworkEvent.Context>> wrappedHandler = (packet, ctx) -> {
            Supplier<NetworkEvent.Context> contextSupplier = ctx;
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (completePackets != null && !completePackets.isEmpty()) {
                MSG mergedPacket = SplitPacket.merge(completePackets);
                if (mergedPacket != null) {
                    ctx.get().enqueueWork(() -> handler.accept(mergedPacket, contextSupplier));
                }
            }
            ctx.get().setPacketHandled(true);
        };
        register(packetClass, encoder, decoder, wrappedHandler);
    }
}
