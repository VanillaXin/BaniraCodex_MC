package xin.vanilla.banira.common.util;

import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkRegistry;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.internal.mixin.accessors.NetworkRegistryAccessor;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.network.BaniraNetworkChannel;

import java.util.List;

@Accessors(fluent = true)
public final class PacketUtils {
    private PacketUtils() {
    }

    public static void broadcastPacket(IPacket<?> packet) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                player.connection.send(packet)
        );
    }

    public static <MSG extends INetworkPacket> void broadcastPacket(MSG msg) {
        BaniraNetworkChannel channel = msg.networkChannel();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendPacketToPlayer(channel, msg, player)
        );
    }

    public static <T extends SplitPacket & INetworkPacket> void broadcastSplitPacket(T packet) {
        BaniraNetworkChannel channel = packet.networkChannel();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
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

    @OnlyIn(Dist.CLIENT)
    private static <MSG extends INetworkPacket> void sendPacketToServer(BaniraNetworkChannel channel, MSG msg) {
        if (!hasChannel(channel)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!(msg instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(mc.player, channel.modId())) {
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

    @OnlyIn(Dist.CLIENT)
    private static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(BaniraNetworkChannel channel, T packet) {
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
        return hasChannel(NetworkInit.HANDLER);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasChannel(BaniraNetworkChannel channel) {
        return hasChannel(channel.channelName());
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasChannel(ResourceLocation channel) {
        init();
        return NETWORK_REGISTRY.banira$instances().containsKey(channel);
    }

    public static boolean hasChannel(ServerPlayerEntity player, BaniraNetworkChannel channel) {
        return hasChannel(player, channel.channelName());
    }

    public static boolean hasChannel(ServerPlayerEntity player, ResourceLocation channel) {
        init();
        return NETWORK_REGISTRY.banira$instances().containsKey(channel);
    }
}
