package xin.vanilla.banira.common.api;

/**
 * 通用的虚拟指令权限类型接口
 * <p>
 * 所有希望接入 {@link xin.vanilla.banira.common.util.VirtualPermissionManager}
 * 的指令枚举都应实现本接口
 * <p>
 */
public interface IVirtualPermissionType {

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
