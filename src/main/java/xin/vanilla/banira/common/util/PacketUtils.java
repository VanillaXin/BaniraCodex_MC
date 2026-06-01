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
import xin.vanilla.banira.common.network.NetworkHandler;
import xin.vanilla.banira.common.network.SplitPacket;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.internal.mixin.accessors.NetworkRegistryAccessor;
import xin.vanilla.banira.internal.network.NetworkInit;

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
        NetworkHandler handler = msg.networkHandler();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendPacketToPlayer(handler, msg, player)
        );
    }

    public static <T extends SplitPacket & INetworkPacket> void broadcastSplitPacket(T packet) {
        NetworkHandler handler = packet.networkHandler();
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player ->
                sendSplitPacketToPlayer(handler, packet, player)
        );
    }

    public static <MSG extends INetworkPacket> void sendPacketToServer(MSG msg) {
        sendPacketToServer(msg.networkHandler(), msg);
    }

    public static <MSG extends INetworkPacket> void sendPacketToPlayer(MSG msg, ServerPlayerEntity player) {
        sendPacketToPlayer(msg.networkHandler(), msg, player);
    }

    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(T packet, ServerPlayerEntity player) {
        sendSplitPacketToPlayer(packet.networkHandler(), packet, player);
    }

    public static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(T packet) {
        sendSplitPacketToServer(packet.networkHandler(), packet);
    }

    @OnlyIn(Dist.CLIENT)
    private static <MSG extends INetworkPacket> void sendPacketToServer(NetworkHandler handler, MSG msg) {
        if (!hasChannel(handler)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (!(msg instanceof ModLoadedToBoth) && !PlayerUtils.isRemoteServerModInstalled(mc.player, handler.getModId())) {
            return;
        }
        handler.sendToServer(msg);
    }

    private static <MSG extends INetworkPacket> void sendPacketToPlayer(NetworkHandler handler, MSG msg, ServerPlayerEntity player) {
        if (!hasChannel(player, handler)) {
            return;
        }
        if (!PlayerUtils.isRemoteClientModInstalled(player, handler.getModId())) {
            return;
        }
        handler.sendToPlayer(player, msg);
    }

    private static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToPlayer(NetworkHandler handler, T packet, ServerPlayerEntity player) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToPlayer(handler, splitPacket, player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static <T extends SplitPacket & INetworkPacket> void sendSplitPacketToServer(NetworkHandler handler, T packet) {
        List<T> splitPackets = packet.split();
        for (T splitPacket : splitPackets) {
            sendPacketToServer(handler, splitPacket);
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
    public static boolean hasChannel(NetworkHandler handler) {
        return hasChannel(handler.getChannelName());
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasChannel(ResourceLocation channel) {
        init();
        return NETWORK_REGISTRY.banira$instances().containsKey(channel);
    }

    public static boolean hasChannel(ServerPlayerEntity player, NetworkHandler handler) {
        return hasChannel(player, handler.getChannelName());
    }

    public static boolean hasChannel(ServerPlayerEntity player, ResourceLocation channel) {
        init();
        return NETWORK_REGISTRY.banira$instances().containsKey(channel);
    }
}
