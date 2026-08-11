package xin.vanilla.banira.platform;

/**
 * 不暴露版本玩家类型的权限判断服务。
 */
public interface BaniraPermissionService {

    boolean hasVanillaPermission(Object player, int permissionLevel);

    boolean hasVirtualPermission(Object player, String permissionKey);
}
