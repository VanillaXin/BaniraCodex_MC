package xin.vanilla.banira.common.api;

import xin.vanilla.banira.api.permission.BaniraVirtualPermission;

/**
 * 通用的虚拟指令权限类型接口
 * <p>
 * 所有希望接入 {@link xin.vanilla.banira.common.util.VirtualPermissionManager}
 * 的指令枚举都应实现本接口
 * <p>
 */
public interface IVirtualPermissionType extends BaniraVirtualPermission {
}
