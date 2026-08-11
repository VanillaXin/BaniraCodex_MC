package xin.vanilla.banira.internal.fabric.platform;

import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.platform.BaniraPermissionService;

/** Fabric 1.16.5 的玩家权限适配。 */
final class FabricBaniraPermissionService implements BaniraPermissionService {
    static final FabricBaniraPermissionService INSTANCE = new FabricBaniraPermissionService();

    private FabricBaniraPermissionService() {
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
