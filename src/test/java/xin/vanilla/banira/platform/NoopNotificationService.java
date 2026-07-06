package xin.vanilla.banira.platform;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;

/**
 * 测试用通知服务，避免默认 platform 触碰客户端 GUI。
 */
public enum NoopNotificationService implements BaniraNotificationService {
    INSTANCE;

    @Override
    public void show(Component component) {
    }

    @Override
    public void show(NotificationData notification) {
    }

    @Override
    public void show(NotificationData notification, boolean fromNetwork) {
    }
}
