package xin.vanilla.banira.api.permission;

/**
 * 子 mod 自定义虚拟权限类型的稳定接口。
 */
public interface BaniraVirtualPermission {

    String modId();

    /**
     * 权限逻辑 id；完整权限键会被规范为 {@code modId:id}。
     */
    String id();

    /**
     * 是否参与虚拟权限管理。
     */
    boolean op();

    /**
     * 展示排序值，例如帮助列表或权限列表。
     */
    int sort();

    default String key() {
        return BaniraVirtualPermissions.key(this);
    }
}
