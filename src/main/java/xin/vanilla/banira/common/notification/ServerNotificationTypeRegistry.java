package xin.vanilla.banira.common.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端已登记的通知类型 id。请在服务端通用初始化时 {@link #register} 声明本 Mod 会发送的类型；
 * 此外 {@link xin.vanilla.banira.common.util.MessageUtils#sendNotification} 在发包时也会自动 {@link #ensureKnown}。
 */
public final class ServerNotificationTypeRegistry {

    private static final Set<String> KNOWN = ConcurrentHashMap.newKeySet();

    static {
        KNOWN.add(NotificationTypeKeys.DEFAULT);
    }

    private ServerNotificationTypeRegistry() {
    }

    /**
     * 在服务端注册一种通知类型
     */
    public static void register(String typeId) {
        KNOWN.add(NotificationTypeKeys.normalizeOrDefault(typeId));
    }

    public static void ensureKnown(String typeId) {
        KNOWN.add(NotificationTypeKeys.normalizeOrDefault(typeId));
    }

    /**
     * 发往客户端的排序副本
     */
    public static List<String> sortedSnapshot() {
        List<String> list = new ArrayList<>(KNOWN);
        Collections.sort(list);
        return list;
    }
}
