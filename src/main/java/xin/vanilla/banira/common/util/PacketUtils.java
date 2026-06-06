package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Accessors(fluent = true)
public final class PacketUtils {
    private PacketUtils() {
    }

    /**
     * 分片网络包缓存
     */
    @Getter
    private static final Map<String, List<? extends SplitPacket>> packetCache = new ConcurrentHashMap<>();


    /**
     * 广播数据包至所有玩家
     *
     * @param packet 数据包
     */
    public static void broadcastPacket(Packet<?> packet) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                player.connection.send(packet)
        );
    }

    /**
     * 广播数据包至所有玩家
     */
    public static <MSG extends INetworkPacket> void broadcastPacket(MSG msg) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                BaniraPlatforms.get().networkService().sendToPlayer(msg, player)
        );
    }

    /**
     * 广播分包数据包至所有玩家
     *
     * @param packet 要发送的数据包
     */
    public static <T extends SplitPacket & INetworkPacket> void broadcastSplitPacket(T packet) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
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
    public static <MSG extends INetworkPacket> void sendPacketToPlayer(MSG msg, ServerPlayer player) {
        BaniraPlatforms.get().networkService().sendToPlayer(msg, player);
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param packet 要发送的数据包
     * @param player 目标玩家
     * @param <T>    分包类型
     */
    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(T packet, ServerPlayer player) {
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

    @OnlyIn(Dist.CLIENT)
    public static boolean hasBaniraServer() {
        return BaniraPlatforms.get().networkService().hasDefaultChannel();
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasChannel(ResourceLocation channel) {
        return BaniraPlatforms.get().networkService().hasLocalChannel(channel);
    }

    public static boolean hasChannel(ServerPlayer player, ResourceLocation channel) {
        return BaniraPlatforms.get().networkService().hasPlayerChannel(player, channel);
    }
}
