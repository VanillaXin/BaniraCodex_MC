package xin.vanilla.banira.api.client.notification;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 子 mod 推荐使用的客户端通知入口。
 */
public final class BaniraNotifications {

    private BaniraNotifications() {
    }

    public static void show(@Nonnull Component component) {
        BaniraPlatforms.get().notificationService().show(Objects.requireNonNull(component, "component"));
    }

    public static void show(@Nonnull NotificationData notification) {
        BaniraPlatforms.get().notificationService().show(Objects.requireNonNull(notification, "notification"));
    }
}
