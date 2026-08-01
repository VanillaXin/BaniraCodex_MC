package xin.vanilla.banira.api.permission;

import xin.vanilla.banira.platform.BaniraPermissionService;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * 原版权限等级与 Banira 虚拟权限的稳定判断入口。
 */
public final class BaniraPermissions {
    private BaniraPermissions() {
    }

    public static boolean has(Object player, int permissionLevel, String virtualPermissionKey) {
        if (permissionLevel < 0 || permissionLevel > 4) {
            throw new IllegalArgumentException("Permission level must be between 0 and 4: " + permissionLevel);
        }
        BaniraPermissionService service = BaniraPlatforms.get().permissionService();
        if (service.hasVanillaPermission(player, permissionLevel)) {
            return true;
        }
        String key = virtualPermissionKey == null ? "" : virtualPermissionKey.trim();
        return !key.isEmpty() && service.hasVirtualPermission(player, key);
    }
}
