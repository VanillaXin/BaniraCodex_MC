package xin.vanilla.banira.common.util;

import lombok.experimental.Accessors;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

import java.util.List;

@Accessors(fluent = true)
public final class PacketUtils {
    private PacketUtils() {
    }

    public static void broadcastPacket(IPacket<?> packet) {
        BaniraPlatforms.get().server().broadcastRawPacket(packet);
    }

    public static <MSG extends INetworkPacket> void broadcastPacket(MSG msg) {
        BaniraNetworkChannel channel = msg.networkChannel();
        PlayerUtils.getAllPlayers().forEach(player ->
                sendPacketToPlayer(channel, msg, player)
        );
    }

    public static <T extends SplitPacket & INetworkPacket> void broadcastSplitPacket(T packet) {
        BaniraNetworkChannel channel = packet.networkChannel();
        PlayerUtils.getAllPlayers().forEach(player ->
                sendSplitPacketToPlayer(channel, packet, player)
        );
    }

    public static <MSG extends INetworkPacket> void sendPacketToServer(MSG msg) {
        sendPacketToServer(msg.networkChannel(), msg);
    }

    public static <MSG extends INetworkPacket> void sendPacketToPlayer(MSG msg, ServerPlayerEntity player) {
        sendPacketToPlayer(msg.networkChannel(), msg, player);
    }

    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(T packet, ServerPlayerEntity player) {
        sendSplitPacketToPlayer(packet.networkChannel(), packet, player);
    }

    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(T packet) {
        sendSplitPacketToServer(packet.networkChannel(), packet);
    }

    private static <MSG extends INetworkPacket> void sendPacketToServer(BaniraNetworkChannel channel, MSG msg) {
        if (!hasChannel(channel)) {
            return;
        }
        PlayerEntity player = BaniraPlatforms.get().client().localPlayer();
        if (player == null) {
            return;
        }
        if (!(msg instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(player, channel.modId())) {
            return;
        }
        channel.sendToServer(msg);
    }

    private static <MSG extends INetworkPacket> void sendPacketToPlayer(BaniraNetworkChannel channel, MSG msg, ServerPlayerEntity player) {
        if (!hasChannel(player, channel)) {
            return;
        }
        if (!PlayerUtils.isRemoteClientModInstalled(player, channel.modId())) {
            return;
        }
        channel.sendToPlayer(player, msg);
    }

    private static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(BaniraNetworkChannel channel, T packet, ServerPlayerEntity player) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToPlayer(channel, splitPacket, player);
        }
    }

    private static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(BaniraNetworkChannel channel, T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToServer(channel, splitPacket);
        }
    }

    public static boolean hasBaniraServer() {
        return hasChannel(NetworkInit.HANDLER);
    }

    public static boolean hasChannel(BaniraNetworkChannel channel) {
        return BaniraPlatforms.get().network().hasChannel(channel);
    }

    public static boolean hasChannel(ResourceLocation channel) {
        return BaniraPlatforms.get().network().hasChannel(channel);
    }

    public static boolean hasChannel(ServerPlayerEntity player, BaniraNetworkChannel channel) {
        return BaniraPlatforms.get().network().hasChannel(player, channel);
    }

    public static boolean hasChannel(ServerPlayerEntity player, ResourceLocation channel) {
        return BaniraPlatforms.get().network().hasChannel(player, channel);
    }
}
