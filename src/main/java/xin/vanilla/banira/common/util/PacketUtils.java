package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.internal.mixin.accessors.NetworkRegistryAccessor;
import xin.vanilla.banira.internal.mixin.accessors.SimpleChannelAccessor;
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
    public static <MSG extends NetworkPacket> void broadcastPacket(MSG msg) {
        SimpleChannel channel = msg.channel().get();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendPacketToPlayer(channel, msg, player)
        );
    }

    /**
     * 广播分包数据包至所有玩家
     *
     * @param packet 要发送的数据包
     */
    public static <T extends SplitPacket> void broadcastSplitPacket(T packet) {
        SimpleChannel channel = packet.channel().get();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendSplitPacketToPlayer(channel, packet, player)
        );
    }


    /**
     * 发送数据包至服务器
     */
    public static <MSG extends NetworkPacket> void sendPacketToServer(MSG msg) {
        sendPacketToServer(msg.channel().get(), msg);
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG extends NetworkPacket> void sendPacketToPlayer(MSG msg, ServerPlayer player) {
        sendPacketToPlayer(msg.channel().get(), msg, player);
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param packet 要发送的数据包
     * @param player 目标玩家
     * @param <T>    分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacketToPlayer(T packet, ServerPlayer player) {
        sendSplitPacketToPlayer(packet.channel().get(), packet, player);
    }

    /**
     * 发送分包数据包至服务器
     *
     * @param packet 要发送的数据包
     * @param <T>    分包类型
     */
    public static <T extends SplitPacket> void sendSplitPacketToServer(T packet) {
        sendSplitPacketToServer(packet.channel().get(), packet);
    }


    /**
     * 发送数据包至服务器
     */
    @OnlyIn(Dist.CLIENT)
    private static <MSG extends NetworkPacket> void sendPacketToServer(SimpleChannel channel, MSG msg) {
        if (!hasChannel(channel)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // ModLoadedToBoth 为握手首包，不依赖已记录的远程服务端状态
        if (!(msg instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(mc.player, getModId(channel))) {
            return;
        }

        channel.sendToServer(msg);
    }

    /**
     * 发送数据包至玩家
     */
    private static <MSG extends NetworkPacket> void sendPacketToPlayer(SimpleChannel channel, MSG msg, ServerPlayer player) {
        if (!hasChannel(player, channel)) return;
        if (!PlayerUtils.isRemoteClientModInstalled(player, getModId(channel))) return;
        channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /**
     * 发送分包数据包至玩家
     *
     * @param channel 网络通道
     * @param packet  要发送的数据包
     * @param player  目标玩家
     * @param <T>     分包类型
     */
    private static <T extends SplitPacket> void sendSplitPacketToPlayer(SimpleChannel channel, T packet, ServerPlayer player) {
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
    private static <T extends SplitPacket> void sendSplitPacketToServer(SimpleChannel channel, T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToServer(channel, splitPacket);
        }
    }


    private static NetworkRegistryAccessor NETWORK_REGISTRY = null;

    private static void init() {
        if (NETWORK_REGISTRY == null) {
            NETWORK_REGISTRY = (NetworkRegistryAccessor) new NetworkRegistry();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasBaniraServer() {
        return hasChannel(NetworkInit.HANDLER.getChannel());
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasChannel(SimpleChannel channel) {
        return hasChannel(getChannelName(channel));
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasChannel(ResourceLocation channel) {
        init();
        return NETWORK_REGISTRY.banira$instances().containsKey(channel);
    }

    public static boolean hasChannel(ServerPlayer player, SimpleChannel channel) {
        return hasChannel(player, getChannelName(channel));
    }

    public static boolean hasChannel(ServerPlayer player, ResourceLocation channel) {
        init();
        return NETWORK_REGISTRY.banira$instances().containsKey(channel);
    }

    public static ResourceLocation getChannelName(SimpleChannel channel) {
        return ((SimpleChannelAccessor) channel).banira$instance().getChannelName();
    }

    public static String getModId(SimpleChannel channel) {
        return getChannelName(channel).getNamespace();
    }
}
