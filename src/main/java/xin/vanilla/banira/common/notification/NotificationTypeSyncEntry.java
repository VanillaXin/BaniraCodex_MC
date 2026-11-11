package xin.vanilla.banira.common.notification;

import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;

/**
 * 登录时服务端向客户端同步的一条通知类型信息
 */
public final class NotificationTypeSyncEntry {

    private final String typeId;
    /**
     * 为 null 表示服务端未登记该类型的客户端默认展示方式，由客户端本地 {@code register} 或 OVERLAY 决定
     */
    private final EnumNotificationTypeDisplayMode defaultDisplayIfAbsent;
    private final Boolean important;

    public NotificationTypeSyncEntry(String typeId, EnumNotificationTypeDisplayMode defaultDisplayIfAbsent) {
        this(typeId, defaultDisplayIfAbsent, null);
    }

    public NotificationTypeSyncEntry(String typeId, EnumNotificationTypeDisplayMode defaultDisplayIfAbsent, Boolean important) {
        this.typeId = NotificationTypeKeys.normalizeOrDefault(typeId);
        this.defaultDisplayIfAbsent = defaultDisplayIfAbsent;
        this.important = important;
    }

    public Boolean important() {
        return important;
    }

    public String typeId() {
        return typeId;
    }

    public EnumNotificationTypeDisplayMode defaultDisplayIfAbsent() {
        return defaultDisplayIfAbsent;
    }
}
