package xin.vanilla.banira.common.api;

import xin.vanilla.banira.api.permission.BaniraVirtualPermission;

/**
 * 旧虚拟指令权限类型接口；子 mod 推荐实现 {@link BaniraVirtualPermission}。
 * <p>
 * 保留本接口是为了让 Banira 内部旧指令枚举继续工作。
 */
public interface IVirtualPermissionType extends BaniraVirtualPermission {

    String modId();

    /**
     * 指令ID
     */
    String id();

    /**
     * 是否参与虚拟权限管理
     */
    boolean op();

    /**
     * 排序值，用于界面展示（例如帮助列表）
     */
    int sort();
}
