package xin.vanilla.banira.internal.forge.network;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.util.IIdentifier;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ForgeNetworkChannel implements BaniraNetworkChannel {
    private static final String PROTOCOL_VERSION = "1";

    private final SimpleChannel channel;
    private final ResourceLocation channelName;
    private final String modId;
    private int nextPacketId = 0;

    public static ForgeNetworkChannel create(String channelName, IIdentifier identifier) {
        ResourceLocation id = identifier.create(channelName);
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                id,
                () -> PROTOCOL_VERSION,
                clientVersion -> true,
                serverVersion -> true
        );
        return new ForgeNetworkChannel(channel, id);
    }

    private ForgeNetworkChannel(SimpleChannel channel, ResourceLocation channelName) {
        this.channel = channel;
        this.channelName = channelName;
        this.modId = channelName.getNamespace();
    }

    @Override
    public ResourceLocation channelName() {
        return channelName;
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public void sendToServer(INetworkPacket packet) {
        channel.sendToServer(packet);
    }

    @Override
    public void sendToPlayer(ServerPlayerEntity player, INetworkPacket packet) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public <MSG extends INetworkPacket> void register(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        registerForge(packetClass,
                (msg, buffer) -> encoder.accept(msg, wrap(buffer)),
                buffer -> decoder.apply(wrap(buffer)),
                (msg, context) -> handler.accept(msg, wrap(context)));
    }

    @Override
    public <MSG extends SplitPacket & INetworkPacket> void registerSplit(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        registerSplitForge(packetClass,
                (msg, buffer) -> encoder.accept(msg, wrap(buffer)),
                buffer -> decoder.apply(wrap(buffer)),
                (msg, context) -> handler.accept(msg, wrap(context)));
    }

    private <MSG extends INetworkPacket> void registerForge(
            Class<MSG> packetClass,
            BiConsumer<MSG, PacketBuffer> encoder,
            Function<PacketBuffer, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        channel.registerMessage(
                nextPacketId++,
                packetClass,
                encoder,
                decoder,
                handler
        );
    }

    private <MSG extends SplitPacket & INetworkPacket> void registerSplitForge(
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
        registerForge(packetClass, encoder, decoder, wrappedHandler);
    }

    private static BaniraPacketBuffer wrap(PacketBuffer buffer) {
        return new BaniraPacketBuffer(new ForgePacketBufferDelegate(buffer));
    }

    private static BaniraNetworkContext wrap(Supplier<NetworkEvent.Context> context) {
        return new BaniraNetworkContext(new ForgeNetworkContextDelegate(context));
    }
}
