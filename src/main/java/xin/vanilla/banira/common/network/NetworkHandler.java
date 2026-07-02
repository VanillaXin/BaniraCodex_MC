package xin.vanilla.banira.common.network;

import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 网络包注册门面；每个 handler 独立维护 packet id 顺序。
 */
public class NetworkHandler {
    private final NetworkPacketRegistrar registrar;
    private int nextPacketId = 0;

    /**
     * 创建跨加载器网络处理器，调用方不需要直接接触具体加载器的 channel/registrar。
     */
    public static NetworkHandler create(String channelName, BaniraIdentifier identifier) {
        return new NetworkHandler(BaniraPlatforms.get().networkService().registrar(channelName, identifier));
    }

    NetworkHandler(NetworkPacketRegistrar registrar) {
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
