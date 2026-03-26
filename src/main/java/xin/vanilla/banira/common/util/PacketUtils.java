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
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.network.packet.SplitPacket;

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
    public static void broadcastPayload(CustomPacketPayload payload) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendPayloadToPlayer(player, payload)
        );
    }

    /**
     * 发送载荷至服务器
     */
    @OnlyIn(Dist.CLIENT)
    public static void sendPacketToServer(CustomPacketPayload msg) {
        if (!hasChannel(msg.type().id())) {
            return;
        }
        PacketDistributor.sendToServer(msg);
    }

    /**
     * 发送载荷至玩家
     */
    public static void sendPacketToPlayer(ServerPlayer player, CustomPacketPayload msg) {
        sendPayloadToPlayer(player, msg);
    }

    private static void sendPayloadToPlayer(ServerPlayer player, CustomPacketPayload msg) {
        if (!hasChannel(player, msg.type().id())) {
            return;
        }
        PacketDistributor.sendToPlayer(player, msg);
    }

    /**
     * 发送分包载荷至玩家
     */
    public static <T extends SplitPacket & CustomPacketPayload> void sendSplitPacketToPlayer(ServerPlayer player, T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToPlayer(player, splitPacket);
        }
    }

    /**
     * 发送分包载荷至服务器
     */
    @OnlyIn(Dist.CLIENT)
    public static <T extends SplitPacket & CustomPacketPayload> void sendSplitPacketToServer(T packet) {
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
    @SuppressWarnings("UnstableApiUsage")
    public static boolean hasChannel(ResourceLocation payloadId) {
        var listener = Minecraft.getInstance().getConnection();
        return listener != null && NetworkRegistry.hasChannel(listener, payloadId);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean hasChannel(ServerPlayer player, ResourceLocation payloadId) {
        return NetworkRegistry.hasChannel(player.connection, payloadId);
    }
}
