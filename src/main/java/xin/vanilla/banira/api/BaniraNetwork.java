package xin.vanilla.banira.api;

import xin.vanilla.banira.platform.BaniraNetworkPacket;
import xin.vanilla.banira.common.network.NetworkPacketRegistrar;
import xin.vanilla.banira.common.util.IIdentifier;

import javax.annotation.Nonnull;

/**
 * 子 mod 推荐使用的网络发送与通道状态入口。
 */
public final class BaniraNetwork {

    private BaniraNetwork() {
    }

    @Nonnull
    public static NetworkPacketRegistrar registrar(@Nonnull String channelName, @Nonnull IIdentifier identifier) {
        return Banira.platform().networkService().registrar(channelName, identifier);
    }

    public static void sendToServer(@Nonnull BaniraNetworkPacket packet) {
        Banira.platform().networkService().sendToServer(packet);
    }

    public static void sendToPlayer(@Nonnull BaniraNetworkPacket packet, @Nonnull Object player) {
        Banira.platform().networkService().sendToPlayer(packet, player);
    }

    public static boolean hasBaniraServer() {
        return Banira.platform().networkService().hasDefaultChannel();
    }

    public static boolean hasLocalChannel(@Nonnull String channelId) {
        return Banira.platform().networkService().hasLocalChannel(channelId);
    }

    public static boolean hasPlayerChannel(@Nonnull Object player, @Nonnull String channelId) {
        return Banira.platform().networkService().hasPlayerChannel(player, channelId);
    }
}
