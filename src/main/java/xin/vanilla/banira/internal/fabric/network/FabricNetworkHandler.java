package xin.vanilla.banira.internal.fabric.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.platform.BaniraNetworkPacket;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Fabric channel 适配器；每个 Banira channel 独立维护 packet id。
 */
public final class FabricNetworkHandler implements NetworkPacketRegistrar {
    private final ResourceLocation channel;
    private final String protocolVersion;
    private final Map<Integer, PacketRegistration<?>> byId = new LinkedHashMap<>();
    private final Map<Class<?>, PacketRegistration<?>> byClass = new LinkedHashMap<>();
    private boolean serverReceiverRegistered;
    private boolean clientReceiverRegistered;

    private FabricNetworkHandler(ResourceLocation channel, String protocolVersion) {
        this.channel = channel;
        this.protocolVersion = protocolVersion == null ? "" : protocolVersion;
    }

    public static FabricNetworkHandler create(String channelName, BaniraIdentifier identifier) {
        return create(channelName, identifier, "");
    }

    public static FabricNetworkHandler create(String channelName, BaniraIdentifier identifier, String protocolVersion) {
        FabricNetworkHandler handler = new FabricNetworkHandler(
                new ResourceLocation(identifier.getNamespace(), channelName), protocolVersion);
        FabricNetworkChannels.installDefault(handler);
        return handler;
    }

    @Override
    public synchronized <MSG extends INetworkPacket> void register(int packetId,
                                                                   Class<MSG> packetClass,
                                                                   BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                                   Function<BaniraPacketBuffer, MSG> decoder,
                                                                   BiConsumer<MSG, BaniraNetworkContext> handler) {
        PacketRegistration<MSG> registration = new PacketRegistration<>(packetId, packetClass, encoder, decoder, handler);
        byId.put(packetId, registration);
        byClass.put(packetClass, registration);
        FabricNetworkChannels.registerPacket(packetClass, this);
        registerServerReceiver();
        if (FabricNetworkChannels.clientReceiverAllowed()) {
            registerClientReceiver();
        }
    }

    ResourceLocation channel() {
        return channel;
    }

    FriendlyByteBuf encode(BaniraNetworkPacket packet) {
        PacketRegistration<?> registration = byClass.get(packet.getClass());
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered Banira packet: " + packet.getClass().getName());
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf(protocolVersion);
        buffer.writeVarInt(registration.packetId);
        registration.encodeUntyped(packet, new FabricPacketBuffer(buffer));
        return buffer;
    }

    void registerClientReceiver() {
        if (clientReceiverRegistered) {
            return;
        }
        clientReceiverRegistered = true;
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, listener, buffer, responseSender) ->
                receive(buffer, FabricNetworkContext.client()));
    }

    private void registerServerReceiver() {
        if (serverReceiverRegistered) {
            return;
        }
        serverReceiverRegistered = true;
        ServerPlayNetworking.registerGlobalReceiver(channel, (server, player, handler, buffer, responseSender) ->
                receive(buffer, FabricNetworkContext.server(server, player)));
    }

    private void receive(FriendlyByteBuf buffer, BaniraNetworkContext context) {
        String remoteProtocol = buffer.readUtf();
        if (!protocolVersion.equals(remoteProtocol)) {
            context.markHandled();
            return;
        }
        int packetId = buffer.readVarInt();
        PacketRegistration<?> registration = byId.get(packetId);
        if (registration == null) {
            context.markHandled();
            return;
        }
        registration.decodeAndHandle(new FabricPacketBuffer(buffer), context);
    }

    private static final class PacketRegistration<MSG extends INetworkPacket> {
        private final int packetId;
        private final Class<MSG> packetClass;
        private final BiConsumer<MSG, BaniraPacketBuffer> encoder;
        private final Function<BaniraPacketBuffer, MSG> decoder;
        private final BiConsumer<MSG, BaniraNetworkContext> handler;

        private PacketRegistration(int packetId,
                                   Class<MSG> packetClass,
                                   BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                   Function<BaniraPacketBuffer, MSG> decoder,
                                   BiConsumer<MSG, BaniraNetworkContext> handler) {
            this.packetId = packetId;
            this.packetClass = packetClass;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }

        private void encodeUntyped(BaniraNetworkPacket packet, BaniraPacketBuffer buffer) {
            encoder.accept(packetClass.cast(packet), buffer);
        }

        private void decodeAndHandle(BaniraPacketBuffer buffer, BaniraNetworkContext context) {
            handler.accept(decoder.apply(buffer), context);
        }
    }
}
