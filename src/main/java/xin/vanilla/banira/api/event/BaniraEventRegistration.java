package xin.vanilla.banira.api.event;

/**
 * Banira 事件注册句柄，用于跨加载器注销回调。
 */
public interface BaniraEventRegistration {
    void unregister();
}
