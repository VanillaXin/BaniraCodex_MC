package xin.vanilla.banira.internal.common;

import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.platform.BaniraNotificationService;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 服务端安全的通知桥；只有确认在客户端时才触碰 GUI 相关类。
 */
public enum BaniraNotificationServices implements BaniraNotificationService {
    INSTANCE;

    @Override
    public void show(@Nonnull Component component) {
        Objects.requireNonNull(component, "component");
        if (!isClientRuntime()) {
            return;
        }
        ClientBridge.show(component);
    }

    @Override
    public void show(@Nonnull NotificationData notification) {
        show(notification, false);
    }

    @Override
    public void show(@Nonnull NotificationData notification, boolean fromNetwork) {
        Objects.requireNonNull(notification, "notification");
        if (!isClientRuntime()) {
            return;
        }
        ClientBridge.show(notification, fromNetwork);
    }

    private static boolean isClientRuntime() {
        return BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient();
    }

    private static final class ClientBridge {
        private static void show(Component component) {
            NotificationManager.get().addNotification(Notification.ofComponent(component));
        }

        private static void show(NotificationData notification, boolean fromNetwork) {
            NotificationManager.get().addNotification(Notification.fromData(notification, fromNetwork), fromNetwork);
        }
    }
}
