package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumPosition;

/**
 * Small notification helper for config editor actions.
 */
public final class ConfigEditorNotifier {
    private ConfigEditorNotifier() {
    }

    public static void show(String translationKey, long durationMs, Object... args) {
        Notification notification = Notification.ofComponent(args != null && args.length > 0
                ? BaniraComponent.get().transClientAuto(translationKey, args)
                : BaniraComponent.get().transClientAuto(translationKey));
        notification.position(EnumPosition.TOP_RIGHT).durationTime(durationMs);
        NotificationManager.get().addNotification(notification);
    }
}
