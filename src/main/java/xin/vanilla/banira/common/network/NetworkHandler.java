package xin.vanilla.banira.common.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.util.IIdentifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 网络处理器
 */
public class NetworkHandler {
    private static int nextPacketId = 0;

    private final ResourceLocation channel;
    private final Map<Integer, PacketRegistration<? extends INetworkPacket>> byId = new HashMap<>();
    private final Map<Class<? extends INetworkPacket>, Integer> byClass = new HashMap<>();
    private boolean serverReceiverRegistered;

    /**
     * 创建网络处理器实例
     *
     * @param channelName 通道名称
     * @param IIdentifier 资源工厂
     * @return NetworkHandler 实例
     */
    public static NetworkHandler create(String channelName, IIdentifier IIdentifier) {
        return new NetworkHandler(IIdentifier.create(channelName));
    }

    private NetworkHandler(ResourceLocation channel) {
        this.channel = channel;
    }

    public ResourceLocation channel() {
        return channel;
    }

    /**
     * 注册网络包
     *
     * @param packetClass 包类
     * @param encoder     编码器
     * @param decoder     解码器
     * @param handler     处理器
     * @param <MSG>       包类型
     */
    public <MSG extends INetworkPacket> void register(Class<MSG> packetClass,
                                                      BiConsumer<MSG, FriendlyByteBuf> encoder,
                                                      Function<FriendlyByteBuf, MSG> decoder,
                                                      BiConsumer<MSG, NetworkContext> handler) {
        int id = nextPacketId++;
        byId.put(id, new PacketRegistration<>(encoder, decoder, handler));
        byClass.put(packetClass, id);
    }

    /**
     * 注册网络包
     *
     * @param packetClass 包类
     * @param encoder     编码器
     * @param decoder     解码器
     * @param handler     处理器
     * @param <MSG>       包类型
     */
    public <MSG extends SplitPacket & INetworkPacket> void registerSplit(
            Class<MSG> packetClass,
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, NetworkContext> handler) {
        BiConsumer<MSG, NetworkContext> wrappedHandler = (packet, ctx) -> {
            // 处理分包逻辑
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (completePackets != null && !completePackets.isEmpty()) {
                // 所有分包已接收完成，合并并调用处理器
                MSG mergedPacket = SplitPacket.merge(completePackets);
                if (mergedPacket != null) {
                    ctx.enqueueWork(() -> handler.accept(mergedPacket, ctx));
                }
            }
        };
        register(packetClass, encoder, decoder, wrappedHandler);
    }

    public void registerServerReceiver() {
        if (serverReceiverRegistered) {
            return;
        }
        serverReceiverRegistered = true;
        ServerPlayNetworking.registerGlobalReceiver(channel, (server, player, handler, buf, responseSender) -> {
            receive(buf, new NetworkContext(true, player, server::execute));
        });
    }

    @Environment(EnvType.CLIENT)
    public void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(channel, (client, handler, buf, responseSender) -> {
            receive(buf, new NetworkContext(false, null, client::execute));
        });
    }

    public <MSG extends INetworkPacket> FriendlyByteBuf encode(MSG msg) {
        Integer id = byClass.get(msg.getClass());
        if (id == null) {
            throw new IllegalArgumentException("Unregistered packet: " + msg.getClass().getName());
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(id);
        @SuppressWarnings("unchecked")
        PacketRegistration<MSG> registration = (PacketRegistration<MSG>) byId.get(id);
        registration.encoder.accept(msg, buf);
        return buf;
    }

    @SuppressWarnings("unchecked")
    private void receive(FriendlyByteBuf buf, NetworkContext context) {
        int id = buf.readVarInt();
        PacketRegistration<INetworkPacket> registration = (PacketRegistration<INetworkPacket>) byId.get(id);
        if (registration == null) {
            return;
        }
        INetworkPacket packet = registration.decoder.apply(buf);
        registration.handler.accept(packet, context);
    }

    private static final class PacketRegistration<MSG extends INetworkPacket> {
        private final BiConsumer<MSG, FriendlyByteBuf> encoder;
        private final Function<FriendlyByteBuf, MSG> decoder;
        private final BiConsumer<MSG, NetworkContext> handler;

        private PacketRegistration(BiConsumer<MSG, FriendlyByteBuf> encoder,
                                   Function<FriendlyByteBuf, MSG> decoder,
                                   BiConsumer<MSG, NetworkContext> handler) {
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }
    }
}
