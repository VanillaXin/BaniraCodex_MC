package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.internal.network.NetworkInit;

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
        ResourceLocation channel = msg.channel();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendPacketToPlayer(channel, msg, player)
        );
    }

    /**
     * 广播分包数据包至所有玩家
     *
     * @param packet 要发送的数据包
     */
    public static <T extends SplitPacket & INetworkPacket> void broadcastSplitPacket(T packet) {
        ResourceLocation channel = packet.channel();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendSplitPacketToPlayer(channel, packet, player)
        );
    }


    /**
     * 发送数据包至服务器
     */
    public static <MSG extends INetworkPacket> void sendPacketToServer(MSG msg) {
        sendPacketToServer(msg.channel(), msg);
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG extends INetworkPacket> void sendPacketToPlayer(MSG msg, ServerPlayer player) {
        sendPacketToPlayer(msg.channel(), msg, player);
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param packet 要发送的数据包
     * @param player 目标玩家
     * @param <T>    分包类型
     */
    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(T packet, ServerPlayer player) {
        sendSplitPacketToPlayer(packet.channel(), packet, player);
    }

    /**
     * 发送分包数据包至服务器
     *
     * @param packet 要发送的数据包
     * @param <T>    分包类型
     */
    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(T packet) {
        sendSplitPacketToServer(packet.channel(), packet);
    }


    /**
     * 发送数据包至服务器
     */
    @Environment(EnvType.CLIENT)
    private static <MSG extends INetworkPacket> void sendPacketToServer(ResourceLocation channel, MSG msg) {
        if (!hasChannel(channel)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // ModLoadedToBoth 为握手首包，不依赖已记录的远程服务端状态
        if (!(msg instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(mc.player, getModId(channel))) {
            return;
        }

        ClientPlayNetworking.send(channel, NetworkInit.HANDLER.encode(msg));
    }

    /**
     * 发送数据包至玩家
     */
    private static <MSG extends INetworkPacket> void sendPacketToPlayer(ResourceLocation channel, MSG msg, ServerPlayer player) {
        if (!hasChannel(player, channel)) return;
        if (!PlayerUtils.isRemoteClientModInstalled(player, getModId(channel))) return;
        ServerPlayNetworking.send(player, channel, NetworkInit.HANDLER.encode(msg));
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param player  目标玩家
     * @param <T>     分包类型
     */
    private static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(ResourceLocation channel, T packet, ServerPlayer player) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToPlayer(channel, splitPacket, player);
        }
    }

    /**
     * 发送分包数据包至服务器
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param <T>     分包类型
     */
    private static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(ResourceLocation channel, T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToServer(channel, splitPacket);
        }
    }


    @Environment(EnvType.CLIENT)
    public static boolean hasBaniraServer() {
        return hasChannel(NetworkInit.HANDLER.channel());
    }

    @Environment(EnvType.CLIENT)
    public static boolean hasChannel(ResourceLocation channel) {
        return ClientPlayNetworking.canSend(channel);
    }

    public static boolean hasChannel(ServerPlayer player, ResourceLocation channel) {
        return ServerPlayNetworking.canSend(player, channel);
    }

    public static String getModId(ResourceLocation channel) {
        return channel.getNamespace();
    }
}
