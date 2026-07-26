package xin.vanilla.banira.internal.fabric.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.common.ClientRuntimeBridge;
import xin.vanilla.banira.platform.BaniraNetworkPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fabric 网络发送入口，公共层不直接接触 Client/ServerPlayNetworking。
 */
public final class FabricNetworkChannels {
    private static FabricNetworkHandler defaultHandler;
    private static final Map<Class<?>, FabricNetworkHandler> HANDLERS_BY_PACKET_CLASS = new ConcurrentHashMap<>();

    private FabricNetworkChannels() {
    }

    static void installDefault(FabricNetworkHandler handler) {
        if (defaultHandler == null) {
            defaultHandler = handler;
        }
    }

    static void registerPacket(Class<? extends INetworkPacket> packetClass, FabricNetworkHandler handler) {
        HANDLERS_BY_PACKET_CLASS.putIfAbsent(packetClass, handler);
    }

    static boolean clientReceiverAllowed() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static void registerClientReceivers() {
        if (defaultHandler != null && clientReceiverAllowed()) {
            defaultHandler.registerClientReceiver();
        }
    }

    public static void sendToServer(BaniraNetworkPacket packet) {
        FabricNetworkHandler handler = handlerFor(packet);
        if (handler == null || !hasLocalChannel(handler.channel())) {
            return;
        }
        Player player = ClientRuntimeBridge.localPlayer();
        if (player == null) {
            return;
        }
        if (!(packet instanceof ModLoadedToBoth)
                && !PlayerUtils.isRemoteServerModInstalled(player, handler.channel().getNamespace())) {
            return;
        }
        ClientPlayNetworking.send(handler.encodePayload(packet));
    }

    public static void sendToPlayer(BaniraNetworkPacket packet, ServerPlayer player) {
        FabricNetworkHandler handler = handlerFor(packet);
        if (handler == null || !hasPlayerChannel(player, handler.channel())) {
            return;
        }
        if (!PlayerUtils.isRemoteClientModInstalled(player, handler.channel().getNamespace())) {
            return;
        }
        ServerPlayNetworking.send(player, handler.encodePayload(packet));
    }

    public static boolean hasDefaultChannel() {
        return defaultHandler != null && hasLocalChannel(defaultHandler.channel());
    }

    public static boolean hasLocalChannel(ResourceLocation channel) {
        try {
            return ClientPlayNetworking.canSend(channel);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean hasPlayerChannel(ServerPlayer player, ResourceLocation channel) {
        return ServerPlayNetworking.canSend(player, channel);
    }

    private static FabricNetworkHandler handlerFor(BaniraNetworkPacket packet) {
        FabricNetworkHandler handler = HANDLERS_BY_PACKET_CLASS.get(packet.getClass());
        return handler != null ? handler : defaultHandler;
    }
}
