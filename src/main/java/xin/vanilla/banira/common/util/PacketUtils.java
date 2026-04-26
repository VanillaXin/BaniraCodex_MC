package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;

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
     * 广播自定义载荷至所有玩家
     */
    public static <MSG extends INetworkPacket> void broadcastPayload(MSG msg) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendPayloadToPlayer(player, msg)
        );
    }

    /**
     * 广播分包数据包至所有玩家
     *
     * @param packet 要发送的数据包
     */
    public static <T extends SplitPacket> void broadcastSplitPacket(T packet) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendSplitPacketToPlayer(packet, player)
        );
    }

    /**
     * 发送载荷至服务器
     */
    @OnlyIn(Dist.CLIENT)
    public static <MSG extends INetworkPacket> void sendPacketToServer(MSG msg) {
        if (!hasChannel(msg.type().id())) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // ModLoadedToBoth 为握手首包，不依赖已记录的远程服务端状态
        if (!(msg instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(mc.player, msg.type().id().getNamespace())) {
            return;
        }
        PacketDistributor.sendToServer(msg);
    }

    /**
     * 发送载荷至玩家
     */
    public static <MSG extends INetworkPacket> void sendPacketToPlayer(MSG msg, ServerPlayer player) {
        sendPayloadToPlayer(player, msg);
    }

    /**
     * 发送载荷至玩家
     */
    public static <MSG extends INetworkPacket> void sendPacketToPlayer(ServerPlayer player, MSG msg) {
        sendPayloadToPlayer(player, msg);
    }

    private static <MSG extends INetworkPacket> void sendPayloadToPlayer(ServerPlayer player, MSG msg) {
        if (!hasChannel(player, msg.type().id())) {
            return;
        }
        if (!PlayerUtils.isRemoteClientModInstalled(player, msg.type().id().getNamespace())) return;
        PacketDistributor.sendToPlayer(player, msg);
    }

    /**
     * 发送分包载荷至玩家
     */
    public static <T extends SplitPacket> void sendSplitPacketToPlayer(T packet, ServerPlayer player) {
        sendSplitPacketToPlayer(player, packet);
    }

    /**
     * 发送分包载荷至玩家
     */
    public static <T extends SplitPacket> void sendSplitPacketToPlayer(ServerPlayer player, T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToPlayer(player, splitPacket);
        }
    }

    /**
     * 发送分包载荷至服务器
     */
    @OnlyIn(Dist.CLIENT)
    public static <T extends SplitPacket> void sendSplitPacketToServer(T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToServer(splitPacket);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasBaniraServer() {
        return hasChannel(ModLoadedToBoth.TYPE.id());
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasChannel(CustomPacketPayload msg) {
        return hasChannel(msg.type().id());
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("UnstableApiUsage")
    public static boolean hasChannel(ResourceLocation payloadId) {
        var listener = Minecraft.getInstance().getConnection();
        return listener != null && NetworkRegistry.hasChannel(listener, payloadId);
    }

    public static boolean hasChannel(ServerPlayer player, CustomPacketPayload msg) {
        return hasChannel(player, msg.type().id());
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean hasChannel(ServerPlayer player, ResourceLocation payloadId) {
        return NetworkRegistry.hasChannel(player.connection, payloadId);
    }
}
