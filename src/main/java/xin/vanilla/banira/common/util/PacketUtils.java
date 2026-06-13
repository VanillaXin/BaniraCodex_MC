package xin.vanilla.banira.common.util;

import lombok.experimental.Accessors;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.util.List;

@Accessors(fluent = true)
public final class PacketUtils {
    private PacketUtils() {
    }

    public static void broadcastPacket(IPacket<?> packet) {
        BaniraPlatforms.get().server().broadcastRawPacket(packet);
    }

    public static <MSG extends INetworkPacket> void broadcastPacket(MSG msg) {
        PlayerUtils.getAllPlayers().forEach(player ->
                sendPacketToPlayer(msg, player)
        );
    }

    public static <T extends SplitPacket & INetworkPacket> void broadcastSplitPacket(T packet) {
        PlayerUtils.getAllPlayers().forEach(player ->
                sendSplitPacketToPlayer(packet, player)
        );
    }

    public static <MSG extends INetworkPacket> void sendPacketToServer(MSG msg) {
        BaniraPlatforms.get().networkService().sendToServer(msg);
    }

    public static <MSG extends INetworkPacket> void sendPacketToPlayer(MSG msg, ServerPlayerEntity player) {
        BaniraPlatforms.get().networkService().sendToPlayer(msg, player);
    }

    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(T packet, ServerPlayerEntity player) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToPlayer(splitPacket, player);
        }
    }

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
        return BaniraPlatforms.get().networkService().hasLocalChannel(channel);
    }

    public static boolean hasChannel(ServerPlayerEntity player, ResourceLocation channel) {
        return BaniraPlatforms.get().networkService().hasPlayerChannel(player, channel);
    }
}
