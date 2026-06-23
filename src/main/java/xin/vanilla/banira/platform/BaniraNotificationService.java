package xin.vanilla.banira.platform;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;

import javax.annotation.Nonnull;

/**
 * 加载器无关的客户端通知服务；渲染、日志和点击处理留在客户端内部实现。
 */
public interface BaniraNotificationService {

    void show(@Nonnull Component component);

    void show(@Nonnull NotificationData notification);

    /**
     * 内部网络入口使用 fromNetwork，让客户端按本地主题解析服务端通知样式。
     */
    void show(@Nonnull NotificationData notification, boolean fromNetwork);
}
