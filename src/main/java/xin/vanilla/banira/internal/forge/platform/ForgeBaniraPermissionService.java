package xin.vanilla.banira.internal.forge.platform;

import net.minecraft.entity.player.PlayerEntity;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.platform.BaniraPermissionService;

/**
 * Forge 1.16.5 玩家权限适配，同时支持客户端同步状态与服务端权威状态。
 */
final class ForgeBaniraPermissionService implements BaniraPermissionService {
    static final ForgeBaniraPermissionService INSTANCE = new ForgeBaniraPermissionService();

    private ForgeBaniraPermissionService() {
    }

    @Override
    public boolean hasVanillaPermission(Object player, int permissionLevel) {
        return player instanceof PlayerEntity && ((PlayerEntity) player).hasPermissions(permissionLevel);
    }

    @Override
    public boolean hasVirtualPermission(Object player, String permissionKey) {
        return CommandUtils.hasVirtualPermission(player, permissionKey);
    }
}
