package xin.vanilla.banira.api.notification;

import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 服务端通知类型登记入口；子 mod 用它声明会发送的通知类型与默认布局。
 */
public final class BaniraNotificationTypes {

    public static final String DEFAULT = NotificationTypeKeys.DEFAULT;

    private BaniraNotificationTypes() {
    }

    @Nonnull
    public static String normalizeOrDefault(String typeId) {
        return NotificationTypeKeys.normalizeOrDefault(typeId);
    }

    public static void register(@Nonnull String typeId) {
        ServerNotificationTypeRegistry.register(typeId);
    }

    public static void register(@Nonnull String typeId, EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        ServerNotificationTypeRegistry.register(typeId, clientDefaultDisplayIfAbsent);
    }

    public static void register(@Nonnull String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation) {
        ServerNotificationTypeRegistry.register(typeId, defaultPosition, defaultAnimation);
    }

    public static void register(@Nonnull String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation,
                                EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        ServerNotificationTypeRegistry.register(typeId, defaultPosition, defaultAnimation, clientDefaultDisplayIfAbsent);
    }

    public static boolean unregister(@Nonnull String typeId) {
        return ServerNotificationTypeRegistry.unregister(typeId);
    }

    @Nonnull
    public static EnumPosition defaultPosition(@Nonnull String typeId) {
        return ServerNotificationTypeRegistry.defaultPosition(typeId);
    }

    @Nonnull
    public static EnumMoveType defaultAnimation(@Nonnull String typeId) {
        return ServerNotificationTypeRegistry.defaultAnimation(typeId);
    }

    @Nonnull
    public static List<String> sortedSnapshot() {
        return ServerNotificationTypeRegistry.sortedSnapshot();
    }
}
