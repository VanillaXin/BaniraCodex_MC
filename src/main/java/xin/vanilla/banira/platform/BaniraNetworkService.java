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

    void sendToServer(@Nonnull BaniraNetworkPacket packet);

    void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player);

    boolean hasDefaultChannel();

    boolean hasLocalChannel(@Nonnull String channelId);

    boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId);
}
