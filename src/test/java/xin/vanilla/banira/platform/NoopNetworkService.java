package xin.vanilla.banira.platform;

import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.IIdentifier;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 测试用网络服务，避免平台单元测试绑定具体加载器。
 */
public enum NoopNetworkService implements BaniraNetworkService {
    INSTANCE;

    @Override
    public NetworkPacketRegistrar registrar(String channelName, IIdentifier identifier) {
        return new NetworkPacketRegistrar() {
            @Override
            public <MSG extends INetworkPacket> void register(int packetId,
                                                              Class<MSG> packetClass,
                                                              BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                                              Function<BaniraPacketBuffer, MSG> decoder,
                                                              BiConsumer<MSG, BaniraNetworkContext> handler) {
                throw new UnsupportedOperationException("Noop network registrar");
            }
        };
    }

    @Override
    public void sendToServer(BaniraNetworkPacket packet) {
    }

    @Override
    public void sendToPlayer(BaniraNetworkPacket packet, Object player) {
    }

    @Override
    public boolean hasDefaultChannel() {
        return false;
    }

    @Override
    public boolean hasLocalChannel(String channelId) {
        return false;
    }

    @Override
    public boolean hasPlayerChannel(Object player, String channelId) {
        return false;
    }
}
