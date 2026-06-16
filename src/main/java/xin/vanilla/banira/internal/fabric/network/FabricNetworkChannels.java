package xin.vanilla.banira.internal.fabric.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.platform.BaniraNetworkPacket;

/**
 * Fabric 网络发送入口，公共层不直接接触 Client/ServerPlayNetworking。
 */
public final class FabricNetworkChannels {
    private static FabricNetworkHandler defaultHandler;

    private FabricNetworkChannels() {
    }

    static void installDefault(FabricNetworkHandler handler) {
        if (defaultHandler == null) {
            defaultHandler = handler;
        }
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
        if (defaultHandler == null || !hasLocalChannel(defaultHandler.channel())) {
            return;
        }
        if (BaniraClientRuntime.localPlayer() == null) {
            return;
        }
        if (!(packet instanceof ModLoadedToBoth)
                && !PlayerUtils.isRemoteServerModInstalled(BaniraClientRuntime.localPlayer(), defaultHandler.channel().getNamespace())) {
            return;
        }
        ClientPlayNetworking.send(defaultHandler.channel(), defaultHandler.encode(packet));
    }

    public static void sendToPlayer(BaniraNetworkPacket packet, ServerPlayer player) {
        if (defaultHandler == null || !hasPlayerChannel(player, defaultHandler.channel())) {
            return;
        }
        if (!PlayerUtils.isRemoteClientModInstalled(player, defaultHandler.channel().getNamespace())) {
            return;
        }
        ServerPlayNetworking.send(player, defaultHandler.channel(), defaultHandler.encode(packet));
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
}
