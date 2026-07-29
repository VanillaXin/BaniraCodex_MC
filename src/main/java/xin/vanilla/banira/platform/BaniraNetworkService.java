package xin.vanilla.banira.platform;

import xin.vanilla.banira.api.BaniraIdentifier;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;

import javax.annotation.Nonnull;

/**
 * 当前加载器的网络能力入口。
 * <p>
 * 公开签名避免直接暴露玩家/通道的版本映射类，具体转换留给 loader adapter。
 */
public interface BaniraNetworkService {
    @Nonnull
    NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull BaniraIdentifier identifier);

    /**
     * 创建带明确协议版本的子 mod 通道。
     *
     * @param optionalClient 是否允许未安装该通道的客户端加入
     */
    @Nonnull
    default NetworkPacketRegistrar registrar(@Nonnull String channelName,
                                             @Nonnull BaniraIdentifier identifier,
                                             @Nonnull String protocolVersion,
                                             boolean optionalClient) {
        return registrar(channelName, identifier);
    }

    void sendToServer(@Nonnull BaniraNetworkPacket packet);

    void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player);

    boolean hasDefaultChannel();

    boolean hasLocalChannel(@Nonnull String channelId);

    boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId);

    /**
     * 查询远端玩家客户端是否声明安装了指定 mod。
     * 1.0.2 平台实现未提供该能力时按未安装处理。
     */
    default boolean isRemoteClientModInstalled(@Nonnull Object player, @Nonnull String modId) {
        return false;
    }
}
