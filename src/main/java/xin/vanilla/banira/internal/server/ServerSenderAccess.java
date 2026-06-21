package xin.vanilla.banira.internal.server;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.util.PlayerUtils;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 服务端玩家句柄的窄门面；公共工具只处理 Object，版本玩家类型留在 internal 层。
 */
public final class ServerSenderAccess {
    private ServerSenderAccess() {
    }

    @Nullable
    private static ServerPlayer asServerPlayer(Object sender) {
        return sender instanceof ServerPlayer ? (ServerPlayer) sender : null;
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
}
