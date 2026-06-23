package xin.vanilla.banira.common.util;

import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.List;


@Accessors(fluent = true)
public final class PacketUtils {
    private PacketUtils() {
    }

    /**
     * 广播数据包至所有玩家
     */
    public static <MSG extends INetworkPacket> void broadcastPacket(MSG msg) {
        BaniraServerRuntime.players().forEach(player ->
                BaniraPlatforms.get().networkService().sendToPlayer(msg, player)
        );
    }

    /**
     * 广播分包数据包至所有玩家
     *
     * @param packet 要发送的数据包
     */
    public static <T extends SplitPacket & INetworkPacket> void broadcastSplitPacket(T packet) {
        BaniraServerRuntime.players().forEach(player ->
                sendSplitPacketToPlayer(packet, player)
        );
    }


    /**
     * 发送数据包至服务器
     */
    public static <MSG extends INetworkPacket> void sendPacketToServer(MSG msg) {
        BaniraPlatforms.get().networkService().sendToServer(msg);
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG extends INetworkPacket> void sendPacketToPlayer(MSG msg, Object player) {
        BaniraPlatforms.get().networkService().sendToPlayer(msg, player);
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param packet 要发送的数据包
     * @param player 目标玩家
     * @param <T>    分包类型
     */
    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(T packet, Object player) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToPlayer(splitPacket, player);
        }
    }

    /**
     * 发送分包数据包至服务器
     *
     * @param packet 要发送的数据包
     * @param <T>    分包类型
     */
    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToServer(splitPacket);
        }
    }

    public static boolean hasBaniraServer() {
        return BaniraPlatforms.get().networkService().hasDefaultChannel();
    }

    public static boolean hasChannel(ResourceLocation channel) {
        return channel != null && hasChannel(channel.toString());
    }

    public static boolean hasChannel(String channelId) {
        return BaniraPlatforms.get().networkService().hasLocalChannel(channelId);
    }

    public static boolean hasChannel(Object player, ResourceLocation channel) {
        return channel != null && hasChannel(player, channel.toString());
    }

    public static boolean hasChannel(Object player, String channelId) {
        return BaniraPlatforms.get().networkService().hasPlayerChannel(player, channelId);
    }
}
