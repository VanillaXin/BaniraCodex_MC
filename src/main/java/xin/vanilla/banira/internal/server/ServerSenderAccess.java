package xin.vanilla.banira.internal.server;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumNotificationVanillaFallback;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.ConfigEditPermission;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.Translator;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 网络包处理中的服务端玩家窄门面。
 * <p>公共 packet 只持有 Object sender，具体玩家类型留在版本内部边界。</p>
 */
public final class ServerSenderAccess {
    private ServerSenderAccess() {
    }

    @Nullable
    private static ServerPlayer asServerPlayer(Object sender) {
        return sender instanceof ServerPlayer ? (ServerPlayer) sender : null;
    }

    public static boolean canAccessServerConfigEditor(Object sender) {
        ServerPlayer player = asServerPlayer(sender);
        return player != null && ConfigEditPermission.canAccessServerConfigEditor(player);
    }

    public static boolean canModifyConfigEntry(Object sender, @Nullable ConfigEntryDescriptor desc) {
        ServerPlayer player = asServerPlayer(sender);
        return player != null && ConfigEditPermission.canModifyEntry(player, desc);
    }

    public static String language(Object sender) {
        ServerPlayer player = asServerPlayer(sender);
        return player != null ? Translator.getPlayerLanguage(player) : Translator.getServerLanguage();
    }

    @Nullable
    public static UUID uuid(Object sender) {
        ServerPlayer player = asServerPlayer(sender);
        return player != null ? PlayerUtils.getPlayerUUID(player) : null;
    }

    @Nullable
    public static String uuidString(Object sender) {
        UUID uuid = uuid(sender);
        return uuid != null ? uuid.toString() : null;
    }

    public static void sendDefaultNotification(Object sender, Component component, EnumPosition position,
                                               EnumMoveType animation, long durationTimeMs) {
        ServerPlayer player = asServerPlayer(sender);
        if (player != null) {
            MessageUtils.sendDefaultNotification(player, component, position, animation, durationTimeMs);
        }
    }

    public static void sendDefaultNotification(Object sender, Component component, EnumPosition position,
                                               EnumMoveType animation, long durationTimeMs,
                                               EnumNotificationStyle style,
                                               EnumNotificationVanillaFallback vanillaFallback) {
        ServerPlayer player = asServerPlayer(sender);
        if (player != null) {
            MessageUtils.sendDefaultNotification(player, component, position, animation, durationTimeMs, style, vanillaFallback);
        }
    }

    public static void sendPacket(Object sender, INetworkPacket packet) {
        if (asServerPlayer(sender) != null) {
            PacketUtils.sendPacketToPlayer(packet, sender);
        }
    }

    public static void setRemoteClientModInstalled(Object sender, String modid, boolean synced) {
        ServerPlayer player = asServerPlayer(sender);
        if (player != null) {
            PlayerUtils.setRemoteClientModInstalled(player, modid, synced);
        }
    }

    public static void removeRemoteClientDataStatus(Object sender) {
        ServerPlayer player = asServerPlayer(sender);
        if (player != null) {
            PlayerUtils.removeRemoteClientDataStatus(player);
        }
    }
}
