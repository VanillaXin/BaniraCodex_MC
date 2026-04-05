package xin.vanilla.banira.common.notification;

/**
 * 通知类型标识。发送端未指定时使用 {@link #DEFAULT}。
 * <p>
 * 其他 Mod 可注册自定义类型 id（建议使用 {@code modid.category_name} 形式），客户端会按类型合并显示配置。
 */
public final class NotificationTypeKeys {

    public static final String DEFAULT = "default";

    private NotificationTypeKeys() {
    }

    public static String normalizeOrDefault(String typeId) {
        if (typeId == null) {
            return DEFAULT;
        }
        String t = typeId.trim();
        return t.isEmpty() ? DEFAULT : t;
    }
}
