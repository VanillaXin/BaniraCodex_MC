package xin.vanilla.banira.common.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.common.network.packet.SplitPacket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Accessors(fluent = true)
public final class PacketUtils {

    /**
     * 分片网络包缓存
     */
    @Getter
    private static final Map<String, List<? extends SplitPacket>> packetCache = new ConcurrentHashMap<>();

    private PacketUtils() {
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
    public static <MSG> void sendPacketToPlayer(SimpleChannel channel, MSG msg, ServerPlayerEntity player) {
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
    public static <T extends SplitPacket> void sendSplitPacketToPlayer(SimpleChannel channel, T packet, ServerPlayerEntity player) {
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
