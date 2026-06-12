package xin.vanilla.banira.common.network;

import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 网络包注册门面；跨版本差异留给 NetworkPacketRegistrar 和加载器 adapter 处理。
 */
public class NetworkHandler {
    private final NetworkPacketRegistrar registrar;
    private int nextPacketId = 0;

    /**
     * 1.16.5 仍需要旧 channel 作为真实注册目标，后续版本可直接替换为平台 registrar。
     */
    public static NetworkHandler from(BaniraNetworkChannel channel) {
        return new NetworkHandler(new NetworkPacketRegistrar() {
            @Override
            public <MSG extends INetworkPacket> void register(
                    int packetId,
                    Class<MSG> packetClass,
                    BiConsumer<MSG, BaniraPacketBuffer> encoder,
                    Function<BaniraPacketBuffer, MSG> decoder,
                    BiConsumer<MSG, BaniraNetworkContext> handler) {
                channel.register(packetClass, encoder, decoder, handler);
            }
        });
    }

    public NetworkHandler(NetworkPacketRegistrar registrar) {
        this.registrar = registrar;
    }

    public <MSG extends INetworkPacket> void register(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        registrar.register(nextPacketId++, packetClass, encoder, decoder, handler);
    }

    public <MSG extends SplitPacket & INetworkPacket> void registerSplit(
            Class<MSG> packetClass,
            BiConsumer<MSG, BaniraPacketBuffer> encoder,
            Function<BaniraPacketBuffer, MSG> decoder,
            BiConsumer<MSG, BaniraNetworkContext> handler) {
        register(packetClass, encoder, decoder, (packet, context) -> {
            List<MSG> completePackets = SplitPacket.handle(packet);
            if (!completePackets.isEmpty()) {
                MSG mergedPacket = SplitPacket.merge(completePackets);
                if (mergedPacket != null) {
                    handler.accept(mergedPacket, context);
                }
            }
            context.markHandled();
        });
    }
}
