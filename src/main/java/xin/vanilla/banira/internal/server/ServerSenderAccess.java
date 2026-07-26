package xin.vanilla.banira.internal.server;

import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.api.INetworkPacket;
import xin.vanilla.banira.common.config.ConfigEntryDescriptor;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumNotificationVanillaFallback;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.*;

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
    private static ServerPlayerEntity asServerPlayer(Object sender) {
        return sender instanceof ServerPlayerEntity ? (ServerPlayerEntity) sender : null;
    }

    public static boolean canAccessServerConfigEditor(Object sender) {
        return ConfigEditPermission.canAccessServerConfigEditor(sender);
    }

    public static boolean canModifyConfigEntry(Object sender, @Nullable ConfigEntryDescriptor desc) {
        return ConfigEditPermission.canModifyEntry(sender, desc);
    }

    public static String language(Object sender) {
        ServerPlayerEntity player = asServerPlayer(sender);
        return player != null ? Translator.getPlayerLanguage(player) : Translator.getServerLanguage();
    }

    @Nullable
    public static UUID uuid(Object sender) {
        ServerPlayerEntity player = asServerPlayer(sender);
        return player != null ? PlayerUtils.getPlayerUUID(player) : null;
    }

    @Nullable
    public static String uuidString(Object sender) {
        UUID uuid = uuid(sender);
        return uuid != null ? uuid.toString() : null;
    }

    public static void sendDefaultNotification(Object sender, Component component, EnumPosition position,
                                               EnumMoveType animation, long durationTimeMs) {
        ServerPlayerEntity player = asServerPlayer(sender);
        if (player != null) {
            MessageUtils.sendDefaultNotification(player, component, position, animation, durationTimeMs);
        }
    }

    public static void sendDefaultNotification(Object sender, Component component, EnumPosition position,
                                               EnumMoveType animation, long durationTimeMs,
                                               EnumNotificationStyle style,
                                               EnumNotificationVanillaFallback vanillaFallback) {
        ServerPlayerEntity player = asServerPlayer(sender);
        if (player != null) {
            MessageUtils.sendDefaultNotification(player, component, position, animation, durationTimeMs, style, vanillaFallback);
        }
    }

    public static void sendPacket(Object sender, INetworkPacket packet) {
        ServerPlayerEntity player = asServerPlayer(sender);
        if (player != null) {
            PacketUtils.sendPacketToPlayer(packet, player);
        }
    }

    public static void setRemoteClientModInstalled(Object sender, String modid, boolean synced) {
        ServerPlayerEntity player = asServerPlayer(sender);
        if (player != null) {
            PlayerUtils.setRemoteClientModInstalled(player, modid, synced);
        }
    }

    public static void removeRemoteClientDataStatus(Object sender) {
        ServerPlayerEntity player = asServerPlayer(sender);
        if (player != null) {
            PlayerUtils.removeRemoteClientDataStatus(player);
        }
    }
}
