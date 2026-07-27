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
        NotificationTypeRegistry.register(typeId);
    }

    public static void register(@Nonnull String typeId, EnumNotificationTypeDisplayMode defaultIfAbsent) {
        NotificationTypeRegistry.register(typeId, defaultIfAbsent);
    }

    public static void register(@Nonnull String typeId, @Nullable Component tooltip) {
        NotificationTypeRegistry.register(typeId, tooltip);
    }

    public static void register(@Nonnull String typeId, EnumNotificationTypeDisplayMode defaultIfAbsent,
                                @Nullable Component tooltip) {
        NotificationTypeRegistry.register(typeId, defaultIfAbsent, tooltip);
    }

    /**
     * 登记通知类型树中 modId 根分组使用的本地化名称。
     */
    public static void registerModDisplayName(@Nonnull String modId, @Nullable Component displayName) {
        NotificationTypeRegistry.registerModDisplayName(modId, displayName);
    }

    @Nullable
    public static Component tooltip(@Nonnull String typeId) {
        return NotificationTypeRegistry.tooltip(typeId);
    }

    @Nullable
    public static Component modDisplayName(@Nonnull String modId) {
        return NotificationTypeRegistry.modDisplayName(modId);
    }

    @Nullable
    public static EnumNotificationTypeDisplayMode displayDefault(@Nonnull String typeId) {
        return NotificationTypeRegistry.resolvedDisplayDefault(typeId);
    }

    @Nonnull
    public static List<String> knownTypesSorted() {
        return NotificationTypeRegistry.knownTypesSorted();
    }
}
