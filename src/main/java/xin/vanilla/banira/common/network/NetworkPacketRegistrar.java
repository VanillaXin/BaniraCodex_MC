package xin.vanilla.banira.common.network;

import xin.vanilla.banira.common.api.INetworkPacket;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 加载器侧的网络包注册适配接口。
 */
public interface NetworkPacketRegistrar {
    <MSG extends INetworkPacket> void register(int packetId,
                                               Class<MSG> packetClass,
                                               BiConsumer<MSG, BaniraPacketBuffer> encoder,
                                               Function<BaniraPacketBuffer, MSG> decoder,
                                               BiConsumer<MSG, BaniraNetworkContext> handler);

    /**
     * 完成当前 channel 的注册阶段。旧 Forge 版本不需要额外操作，较新 Forge/NeoForge 会在 adapter 内锁定 channel。
     */
    default void complete() {
    }
}
