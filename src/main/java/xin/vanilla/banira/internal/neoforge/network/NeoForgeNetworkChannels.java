package xin.vanilla.banira.internal.neoforge.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.neoforge.client.NeoForgeClientNetworkAccess;
import xin.vanilla.banira.platform.BaniraNetworkPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** NeoForge payload 注册表与发送路由。 */
public final class NeoForgeNetworkChannels {
    private static final Map<String, List<PendingRegistration<?>>> PENDING = new ConcurrentHashMap<>();
    private static final Map<Class<?>, PayloadBinding<?>> BY_PACKET = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> BY_CHANNEL = new ConcurrentHashMap<>();
    private static volatile ResourceLocation defaultChannel;

    private NeoForgeNetworkChannels() {
    }

    static <MSG extends INetworkPacket> void bind(ResourceLocation channel,
                                                   String protocolVersion,
                                                   boolean optionalClient,
                                                   int packetId,
                                                   Class<MSG> packetClass,
                                                   BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                   Function<BaniraPacketBuffer, MSG> decoder,
                                                   BiConsumer<MSG, BaniraNetworkContext> handler) {
        ResourceLocation payloadId = ResourceLocation.fromNamespaceAndPath(
                channel.getNamespace(), channel.getPath() + "/" + packetId);
        CustomPacketPayload.Type<NeoForgePayload<MSG>> type = new CustomPacketPayload.Type<>(payloadId);
        StreamCodec<RegistryFriendlyByteBuf, NeoForgePayload<MSG>> codec = StreamCodec.of(
                (buffer, payload) -> encoder.accept(payload.packet(), new NeoForgePacketBuffer(buffer)),
                buffer -> new NeoForgePayload<>(decoder.apply(new NeoForgePacketBuffer(buffer)), type));
        PendingRegistration<MSG> registration = new PendingRegistration<>(
                protocolVersion, optionalClient, type, codec,
                (payload, context) -> handler.accept(payload.packet(), new NeoForgeNetworkContext(context)));
        PENDING.computeIfAbsent(protocolVersion, key -> Collections.synchronizedList(new ArrayList<>())).add(registration);
        BY_PACKET.put(packetClass, new PayloadBinding<>(channel, type));
        BY_CHANNEL.computeIfAbsent(channel, key -> Collections.synchronizedList(new ArrayList<>())).add(payloadId);
        if (defaultChannel == null) {
            defaultChannel = channel;
        }
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PENDING.forEach((protocol, registrations) -> registrations.forEach(registration -> registration.register(event)));
    }

    @OnlyIn(Dist.CLIENT)
    public static void sendToServer(BaniraNetworkPacket packet) {
        PayloadBinding<?> binding = BY_PACKET.get(packet.getClass());
        if (binding == null || !NeoForgeClientNetworkAccess.hasChannel(binding.type().id())) {
            return;
        }
        if (!(packet instanceof ModLoadedToBoth)) {
            var player = NeoForgeClientNetworkAccess.player();
            if (player == null || !PlayerUtils.isRemoteServerModInstalled(player, binding.channel().getNamespace())) {
                return;
            }
        }
        CustomPacketPayload payload = binding.wrap(packet);
        if (payload != null) {
            PacketDistributor.sendToServer(payload);
        }
    }

    public static void sendToPlayer(BaniraNetworkPacket packet, ServerPlayer player) {
        PayloadBinding<?> binding = BY_PACKET.get(packet.getClass());
        if (binding == null || !NetworkRegistry.hasChannel(player.connection, binding.type().id())) {
            return;
        }
        if (!PlayerUtils.isRemoteClientModInstalled(player, binding.channel().getNamespace())) {
            return;
        }
        CustomPacketPayload payload = binding.wrap(packet);
        if (payload != null) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasDefaultChannel() {
        return defaultChannel != null && channelPayloads(defaultChannel).stream().anyMatch(NeoForgeClientNetworkAccess::hasChannel);
    }

    public static boolean hasLocalChannel(ResourceLocation channel) {
        return BY_CHANNEL.containsKey(channel);
    }

    public static boolean hasPlayerChannel(ServerPlayer player, ResourceLocation channel) {
        return channelPayloads(channel).stream().anyMatch(id -> NetworkRegistry.hasChannel(player.connection, id));
    }

    private static List<ResourceLocation> channelPayloads(ResourceLocation channel) {
        return BY_CHANNEL.getOrDefault(channel, List.of());
    }

    private record PendingRegistration<MSG extends INetworkPacket>(
            String protocolVersion,
            boolean optionalClient,
            CustomPacketPayload.Type<NeoForgePayload<MSG>> type,
            StreamCodec<RegistryFriendlyByteBuf, NeoForgePayload<MSG>> codec,
            BiConsumer<NeoForgePayload<MSG>, IPayloadContext> handler) {
        private void register(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar(protocolVersion);
            if (optionalClient) {
                registrar = registrar.optional();
            }
            registrar.playBidirectional(type, codec, handler::accept);
        }
    }

    private record PayloadBinding<MSG extends INetworkPacket>(
            ResourceLocation channel,
            CustomPacketPayload.Type<NeoForgePayload<MSG>> type) {
        @SuppressWarnings("unchecked")
        private CustomPacketPayload wrap(BaniraNetworkPacket packet) {
            return packet instanceof INetworkPacket ? new NeoForgePayload<>((MSG) packet, type) : null;
        }
    }

    private record NeoForgePayload<MSG extends INetworkPacket>(
            MSG packet,
            CustomPacketPayload.Type<NeoForgePayload<MSG>> payloadType) implements CustomPacketPayload {
        @Override public Type<? extends CustomPacketPayload> type() { return payloadType; }
    }
}
