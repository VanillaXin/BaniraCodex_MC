package xin.vanilla.banira.internal.neoforge.platform;

import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.platform.BaniraPermissionService;

/**
 * NeoForge 玩家权限适配，同时支持客户端同步状态与服务端权威状态。
 */
final class NeoForgeBaniraPermissionService implements BaniraPermissionService {
    static final NeoForgeBaniraPermissionService INSTANCE = new NeoForgeBaniraPermissionService();

    private NeoForgeBaniraPermissionService() {
    }

    @Override
    public boolean hasVanillaPermission(Object player, int permissionLevel) {
        return player instanceof Player && ((Player) player).hasPermissions(permissionLevel);
    }

    @Override
    public boolean hasVirtualPermission(Object player, String permissionKey) {
        return player instanceof Player && CommandUtils.hasVirtualPermission((Player) player, permissionKey);
    }
}
