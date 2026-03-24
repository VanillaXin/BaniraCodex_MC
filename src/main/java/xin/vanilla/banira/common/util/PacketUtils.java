package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.network.packet.SplitPacket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;


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
    public static <MSG> void broadcastPacket(Supplier<SimpleChannel> channel, MSG msg) {
        broadcastPacket(channel.get(), msg);
    }

    /**
     * 广播数据包至所有玩家
     */
    public static <MSG> void broadcastPacket(SimpleChannel channel, MSG msg) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendPacketToPlayer(channel, msg, player)
        );
    }

    /**
     * 广播分包数据包至所有玩家
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     */
    public static <T extends SplitPacket> void broadcastSplitPacket(Supplier<SimpleChannel> channel, T packet) {
        broadcastSplitPacket(channel.get(), packet);
    }

    /**
     * 广播分包数据包至所有玩家
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     */
    public static <T extends SplitPacket> void broadcastSplitPacket(SimpleChannel channel, T packet) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendSplitPacket(channel, packet, PacketDistributor.PLAYER.with(() -> player))
        );
    }


    /**
     * 发送数据包至服务器
     */
    public static <MSG> void sendPacketToServer(Supplier<SimpleChannel> channel, MSG msg) {
        sendPacketToServer(channel.get(), msg);
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG> void sendPacketToPlayer(Supplier<SimpleChannel> channel, MSG msg, ServerPlayer player) {
        sendPacketToPlayer(channel.get(), msg, player);
    }

    /**
     * 发送分包数据包
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param target  发送目标
     * @param <T>     分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacket(Supplier<SimpleChannel> channel, T packet, PacketDistributor.PacketTarget target) {
        sendSplitPacket(channel.get(), packet, target);
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param player  目标玩家
     * @param <T>     分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacketToPlayer(Supplier<SimpleChannel> channel, T packet, ServerPlayer player) {
        sendSplitPacketToPlayer(channel.get(), packet, player);
    }

    /**
     * 发送分包数据包至服务器
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param <T>     分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacketToServer(Supplier<SimpleChannel> channel, T packet) {
        sendSplitPacketToServer(channel.get(), packet);
    }


    /**
     * 发送数据包至服务器
     */
    public static <MSG> void sendPacketToServer(SimpleChannel channel, MSG msg) {
        channel.sendToServer(msg);
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG> void sendPacketToPlayer(SimpleChannel channel, MSG msg, ServerPlayer player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /**
     * 发送分包数据包
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param target  发送目标
     * @param <T>     分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacket(SimpleChannel channel, T packet, PacketDistributor.PacketTarget target) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            channel.send(target, splitPacket);
        }
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param player  目标玩家
     * @param <T>     分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacketToPlayer(SimpleChannel channel, T packet, ServerPlayer player) {
        sendSplitPacket(channel, packet, PacketDistributor.PLAYER.with(() -> player));
    }

    /**
     * 发送分包数据包至服务器
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param <T>     分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacketToServer(SimpleChannel channel, T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            channel.sendToServer(splitPacket);
        }
    }
}
