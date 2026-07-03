package xin.vanilla.banira.common.network;

import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 网络处理器
 */
public class NetworkHandler {
    private final NetworkPacketRegistrar registrar;
    private int nextPacketId = 0;

    /**
     * 创建网络处理器实例
     *
     * @param channelName 通道名称
     * @param identifier  资源工厂
     * @return NetworkHandler 实例
     */
    public static NetworkHandler create(String channelName, BaniraIdentifier identifier) {
        return new NetworkHandler(BaniraPlatforms.get().networkService().registrar(channelName, identifier));
    }

    private NetworkHandler(NetworkPacketRegistrar registrar) {
        this.registrar = registrar;
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
                                                      BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                      Function<BaniraPacketBuffer, MSG> decoder,
                                                      BiConsumer<MSG, BaniraNetworkContext> handler) {
        registrar.register(
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
    public <MSG extends SplitPacket & INetworkPacket> void registerSplit(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        BiConsumer<MSG, BaniraNetworkContext> wrappedHandler = (packet, ctx) -> {
            // 处理分包逻辑
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (completePackets != null && !completePackets.isEmpty()) {
                // 所有分包已接收完成，合并并调用处理器
                MSG mergedPacket = SplitPacket.merge(completePackets);
                if (mergedPacket != null) {
                    handler.accept(mergedPacket, ctx);
                }
            }
            ctx.markHandled();
        };
        register(packetClass, encoder, decoder, wrappedHandler);
    }

    /**
     * 结束当前 channel 的 packet 注册。调用方注册完全部 packet 后调用一次即可。
     */
    public void completeRegistration() {
        registrar.complete();
    }
}
