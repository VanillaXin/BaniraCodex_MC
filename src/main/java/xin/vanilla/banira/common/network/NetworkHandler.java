package xin.vanilla.banira.common.network;

import lombok.Getter;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.common.api.ResourceFactory;
import xin.vanilla.banira.common.network.packet.SplitPacket;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 网络处理器
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static int nextPacketId = 0;

    @Getter
    private final SimpleChannel channel;

    /**
     * 创建网络处理器实例
     *
     * @param channelName     通道名称
     * @param resourceFactory 资源工厂
     * @return NetworkHandler 实例
     */
    public static NetworkHandler create(String channelName, ResourceFactory resourceFactory) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(
                resourceFactory.create(channelName),
                () -> PROTOCOL_VERSION,
                clientVersion -> true,      // 客户端版本始终有效
                serverVersion -> true       // 服务端版本始终有效
        );
        return new NetworkHandler(channel);
    }

    private NetworkHandler(SimpleChannel channel) {
        this.channel = channel;
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
    public <MSG> void register(Class<MSG> packetClass,
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

    /**
     * 注册网络包
     *
     * @param packetClass 包类
     * @param encoder     编码器
     * @param decoder     解码器
     * @param handler     处理器
     * @param <MSG>       包类型
     */
    public <MSG extends SplitPacket> void registerSplit(
            Class<MSG> packetClass,
            BiConsumer<MSG, PacketBuffer> encoder,
            Function<PacketBuffer, MSG> decoder,
            BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        BiConsumer<MSG, Supplier<NetworkEvent.Context>> wrappedHandler = (packet, ctx) -> {
            // 保存原始上下文
            final Supplier<NetworkEvent.Context> contextSupplier = ctx;
            // 处理分包逻辑
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (completePackets != null && !completePackets.isEmpty()) {
                // 所有分包已接收完成，合并并调用处理器
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
