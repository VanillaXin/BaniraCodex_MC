package xin.vanilla.banira.internal.forge.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.SplitPacket;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ForgeNetworkChannel {
    private final SimpleChannel channel;
    private final ResourceLocation channelName;
    private final String modId;

    public static ForgeNetworkChannel create(String channelName, BaniraIdentifier identifier) {
        return create(channelName, identifier, "1", true);
    }

    public static ForgeNetworkChannel create(String channelName, BaniraIdentifier identifier,
                                             String protocolVersion, boolean optionalClient) {
        ResourceLocation id = new ResourceLocation(identifier.getNamespace(), channelName);
        Predicate<String> acceptedVersions = optionalClient
                ? NetworkRegistry.acceptMissingOr(protocolVersion)
                : protocolVersion::equals;
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                id,
                () -> protocolVersion,
                acceptedVersions,
                acceptedVersions
        );
        return new ForgeNetworkChannel(channel, id);
    }

    private ForgeNetworkChannel(SimpleChannel channel, ResourceLocation channelName) {
        this.channel = channel;
        this.channelName = channelName;
        this.modId = channelName.getNamespace();
    }

    public ResourceLocation channelName() {
        return channelName;
    }

    public String modId() {
        return modId;
    }

    public void sendToServer(INetworkPacket packet) {
        channel.sendToServer(packet);
    }

    public void sendToPlayer(ServerPlayerEntity player, INetworkPacket packet) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public <MSG extends INetworkPacket> void register(
            int packetId,
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        registerForge(packetId, packetClass,
                (msg, buffer) -> encoder.accept(msg, wrap(buffer)),
                buffer -> decoder.apply(wrap(buffer)),
                (msg, context) -> handler.accept(msg, wrap(context)));
    }

    public <MSG extends SplitPacket & INetworkPacket> void registerSplit(
            int packetId,
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        registerSplitForge(packetId, packetClass,
                (msg, buffer) -> encoder.accept(msg, wrap(buffer)),
                buffer -> decoder.apply(wrap(buffer)),
                (msg, context) -> handler.accept(msg, wrap(context)));
    }

    private <MSG extends INetworkPacket> void registerForge(
            int packetId,
            Class<MSG> packetClass,
            BiConsumer<MSG, PacketBuffer> encoder,
            Function<PacketBuffer, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        channel.registerMessage(
                packetId,
                packetClass,
                encoder,
                decoder,
                handler
        );
    }

    private <MSG extends SplitPacket & INetworkPacket> void registerSplitForge(
            int packetId,
            Class<MSG> packetClass,
            BiConsumer<MSG, PacketBuffer> encoder,
            Function<PacketBuffer, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        BiConsumer<MSG, Supplier<NetworkEvent.Context>> wrappedHandler = (packet, context) -> {
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (!completePackets.isEmpty()) {
                MSG mergedPacket = SplitPacket.merge(completePackets);
                if (mergedPacket != null) {
                    context.get().enqueueWork(() -> handler.accept(mergedPacket, context));
                }
            }
            context.get().setPacketHandled(true);
        };
        registerForge(packetId, packetClass, encoder, decoder, wrappedHandler);
    }

    private static BaniraPacketBuffer wrap(PacketBuffer buffer) {
        return new ForgePacketBuffer(buffer);
    }

    private static BaniraNetworkContext wrap(Supplier<NetworkEvent.Context> context) {
        return new ForgeNetworkContext(context);
    }
}
