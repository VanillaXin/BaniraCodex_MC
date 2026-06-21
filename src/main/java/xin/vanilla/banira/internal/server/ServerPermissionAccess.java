package xin.vanilla.banira.internal.server;

import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.util.CommandUtils;

/**
 * 服务端玩家权限的内部窄接口，避免公共配置 API 直接暴露版本玩家类型。
 */
public final class ServerPermissionAccess {
    private ServerPermissionAccess() {
    }

    public static boolean hasPermission(Object player, int permissionLevel) {
        ServerPlayerEntity serverPlayer = asServerPlayer(player);
        return serverPlayer != null && serverPlayer.createCommandSourceStack().hasPermission(permissionLevel);
    }

    public static boolean hasVirtualPermission(Object player, String fullPermissionKey) {
        ServerPlayerEntity serverPlayer = asServerPlayer(player);
        return serverPlayer != null && CommandUtils.hasVirtualPermission(serverPlayer, fullPermissionKey);
    }

    private static ServerPlayerEntity asServerPlayer(Object player) {
        return player instanceof ServerPlayerEntity ? (ServerPlayerEntity) player : null;
    }
}
