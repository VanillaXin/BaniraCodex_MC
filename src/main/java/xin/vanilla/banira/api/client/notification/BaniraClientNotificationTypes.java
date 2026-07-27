package xin.vanilla.banira.api.client.notification;

import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * 客户端通知类型登记入口；用于提前让配置界面知道子 mod 的本地通知类型。
 */
public final class BaniraClientNotificationTypes {

    private BaniraClientNotificationTypes() {
    }

    public static void register(@Nonnull String typeId) {
        NotificationTypeRegistry.registerInternal(typeId);
    }

    public static void register(@Nonnull String typeId, EnumNotificationTypeDisplayMode defaultIfAbsent) {
        NotificationTypeRegistry.registerInternal(typeId, defaultIfAbsent);
    }

    public static void register(@Nonnull String typeId, @Nullable Component tooltip) {
        NotificationTypeRegistry.registerInternal(typeId, tooltip);
    }

    public static void register(@Nonnull String typeId, EnumNotificationTypeDisplayMode defaultIfAbsent,
                                @Nullable Component tooltip) {
        NotificationTypeRegistry.registerInternal(typeId, defaultIfAbsent, tooltip);
    }

    public static void registerModDisplayName(@Nonnull String modId, @Nullable Component displayName) {
        NotificationTypeRegistry.registerModDisplayNameInternal(modId, displayName);
    }

    @Nullable
    public static Component tooltip(@Nonnull String typeId) {
        return NotificationTypeRegistry.tooltipInternal(typeId);
    }

    @Nullable
    public static Component modDisplayName(@Nonnull String modId) {
        return NotificationTypeRegistry.modDisplayNameInternal(modId);
    }

    @Nullable
    public static EnumNotificationTypeDisplayMode displayDefault(@Nonnull String typeId) {
        return NotificationTypeRegistry.resolvedDisplayDefaultInternal(typeId);
    }

    @Nonnull
    public static List<String> knownTypesSorted() {
        return NotificationTypeRegistry.knownTypesSortedInternal();
    }
}
