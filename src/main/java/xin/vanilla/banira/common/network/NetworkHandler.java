package xin.vanilla.banira.common.network;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.banira.common.network.packet.SplitPacket;
import xin.vanilla.banira.common.util.IIdentifier;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 网络处理器
 */
public class NetworkHandler {
    private static int nextPacketId = 0;

    @Getter
    private final SimpleChannel channel;

    /**
     * 创建网络处理器实例
     *
     * @param channelName 通道名称
     * @param IIdentifier 资源工厂
     * @return NetworkHandler 实例
     */
    public static NetworkHandler create(String channelName, IIdentifier IIdentifier) {
        SimpleChannel channel = ChannelBuilder.named(IIdentifier.create(channelName))
                .networkProtocolVersion(1)
                .acceptedVersions((status, version) -> true)
                .simpleChannel();
        return new NetworkHandler(channel);
    }

    private NetworkHandler(SimpleChannel channel) {
        this.channel = channel;
    }

    /**
     * 在所有 {@link #register} / {@link #registerSplit} 完成之后调用，完成通道构建；此前不可使用通道发包。
     */
    public void build() {
        channel.build();
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
                               BiConsumer<MSG, FriendlyByteBuf> encoder,
                               Function<FriendlyByteBuf, MSG> decoder,
                               BiConsumer<MSG, CustomPayloadEvent.Context> handler) {
        channel.messageBuilder(packetClass, nextPacketId++)
                .encoder(encoder)
                .decoder(decoder)
                .consumerMainThread((msg, ctx) -> {
                    handler.accept(msg, ctx);
                    ctx.setPacketHandled(true);
                })
                .add();
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
            BiConsumer<MSG, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, MSG> decoder,
            BiConsumer<MSG, CustomPayloadEvent.Context> handler) {
        BiConsumer<MSG, CustomPayloadEvent.Context> wrappedHandler = (packet, ctx) -> {
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (completePackets != null && !completePackets.isEmpty()) {
                MSG mergedPacket = SplitPacket.merge(completePackets);
                if (mergedPacket != null) {
                    ctx.enqueueWork(() -> handler.accept(mergedPacket, ctx));
                }
            }
            ctx.setPacketHandled(true);
        };
        register(packetClass, encoder, decoder, wrappedHandler);
    }
}
