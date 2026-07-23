package xin.vanilla.banira.internal.fabric.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.internal.common.ClientRuntimeBridge;
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
    private final CustomPacketPayload.Type<FabricPayload> payloadType;
    private final Map<Integer, PacketRegistration<?>> byId = new LinkedHashMap<>();
    private final Map<Class<?>, PacketRegistration<?>> byClass = new LinkedHashMap<>();
    private boolean payloadRegistered;
    private boolean serverReceiverRegistered;
    private boolean clientReceiverRegistered;

    private FabricNetworkHandler(ResourceLocation channel) {
        this.channel = channel;
        this.payloadType = new CustomPacketPayload.Type<>(channel);
    }

    public static FabricNetworkHandler create(String channelName, BaniraIdentifier identifier) {
        FabricNetworkHandler handler = new FabricNetworkHandler(ResourceLocation.fromNamespaceAndPath(identifier.getNamespace(), channelName));
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
        registerPayloadType();
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
        FriendlyByteBuf buffer = registryBuffer(Unpooled.buffer());
        buffer.writeVarInt(registration.packetId);
        registration.encodeUntyped(packet, new FabricPacketBuffer(buffer));
        return buffer;
    }

    FabricPayload encodePayload(BaniraNetworkPacket packet) {
        return new FabricPayload(payloadType, encode(packet));
    }

    void registerClientReceiver() {
        if (clientReceiverRegistered) {
            return;
        }
        clientReceiverRegistered = true;
        ClientPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                receive(payload.copyBuffer(), FabricNetworkContext.client()));
    }

    private void registerServerReceiver() {
        if (serverReceiverRegistered) {
            return;
        }
        serverReceiverRegistered = true;
        ServerPlayNetworking.registerGlobalReceiver(payloadType, (payload, context) ->
                receive(payload.copyBuffer(), FabricNetworkContext.server(context.server(), context.player())));
    }

    private synchronized void registerPayloadType() {
        if (payloadRegistered) {
            return;
        }
        payloadRegistered = true;
        StreamCodec<RegistryFriendlyByteBuf, FabricPayload> codec = StreamCodec.of(
                (buffer, payload) -> buffer.writeBytes(payload.copyBuffer()),
                buffer -> new FabricPayload(payloadType, copyReadable(buffer))
        );
        PayloadTypeRegistry.playC2S().register(payloadType, codec);
        PayloadTypeRegistry.playS2C().register(payloadType, codec);
    }

    private static RegistryFriendlyByteBuf copyReadable(RegistryFriendlyByteBuf source) {
        return new RegistryFriendlyByteBuf(Unpooled.copiedBuffer(source.readBytes(source.readableBytes())), source.registryAccess());
    }

    private static RegistryFriendlyByteBuf copyBuffer(FriendlyByteBuf source) {
        RegistryAccess registryAccess = source instanceof RegistryFriendlyByteBuf registryBuffer
                ? registryBuffer.registryAccess()
                : currentRegistryAccess();
        return new RegistryFriendlyByteBuf(Unpooled.copiedBuffer(source.copy()), registryAccess);
    }

    private static RegistryFriendlyByteBuf registryBuffer(ByteBuf source) {
        return new RegistryFriendlyByteBuf(source, currentRegistryAccess());
    }

    private static RegistryAccess currentRegistryAccess() {
        if (BaniraCodex.serverInstance().val()) {
            return BaniraCodex.serverInstance().key().registryAccess();
        }
        try {
            Level level = ClientRuntimeBridge.level();
            if (level != null) {
                return level.registryAccess();
            }
        } catch (NoClassDefFoundError ignored) {
            // Dedicated server never has client classes; fall back to the empty registry below.
        }
        return RegistryAccess.EMPTY;
    }

    private void receive(FriendlyByteBuf buffer, BaniraNetworkContext context) {
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

    private record FabricPayload(CustomPacketPayload.Type<FabricPayload> type, FriendlyByteBuf buffer) implements CustomPacketPayload {
        private FriendlyByteBuf copyBuffer() {
            return FabricNetworkHandler.copyBuffer(buffer);
        }
    }
}
