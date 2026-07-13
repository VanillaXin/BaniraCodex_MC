package xin.vanilla.banira.common.notification;

import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端已登记的通知类型 id。请在服务端通用初始化时 {@link #register} 声明本 Mod 会发送的类型；
 * 此外 {@link xin.vanilla.banira.common.util.MessageUtils#sendNotification(net.minecraft.server.level.ServerPlayer, xin.vanilla.banira.common.data.Component, String)} 在发包时也会自动 {@link #ensureKnown}。
 * <p>
 * 可通过 {@link #register(String, EnumPosition, EnumMoveType)} 为类型指定默认位置与动画；未指定时使用 {@link EnumPosition#TOP_RIGHT} 与 {@link EnumMoveType#AUTO}。
 * <p>
 * 可选登记 {@link EnumNotificationTypeDisplayMode}，随 {@link xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient} 下发：
 * 客户端在本地 JSON 无该类型条目时采用（客户端模组显式 {@code NotificationTypeRegistry.register(id, mode)} 优先）。
 */
public final class ServerNotificationTypeRegistry {

    private static final Set<String> KNOWN = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, TypeLayoutDefaults> LAYOUT_DEFAULTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, EnumNotificationTypeDisplayMode> SYNC_CLIENT_DISPLAY_IF_ABSENT = new ConcurrentHashMap<>();

    private static final TypeLayoutDefaults FALLBACK_LAYOUT = new TypeLayoutDefaults(EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);

    static {
        KNOWN.add(NotificationTypeKeys.DEFAULT);
    }

    private ServerNotificationTypeRegistry() {
    }

    /**
     * 在服务端注册一种通知类型（默认位置 {@link EnumPosition#TOP_RIGHT}、动画 {@link EnumMoveType#AUTO}）
     */
    public static void register(String typeId) {
        KNOWN.add(NotificationTypeKeys.normalizeOrDefault(typeId));
    }

    /**
     * 注册类型并指定：客户端配置文件无该类型条目时的建议展示方式（登录同步）
     */
    public static void register(String typeId, EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        KNOWN.add(key);
        if (clientDefaultDisplayIfAbsent != null) {
            SYNC_CLIENT_DISPLAY_IF_ABSENT.put(key, clientDefaultDisplayIfAbsent);
        }
    }

    /**
     * 在服务端注册通知类型并指定默认布局（用于仅传类型 id 的 {@code sendNotification} 重载）
     */
    public static void register(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        KNOWN.add(key);
        EnumPosition p = defaultPosition != null ? defaultPosition : FALLBACK_LAYOUT.position();
        EnumMoveType a = defaultAnimation != null ? defaultAnimation : FALLBACK_LAYOUT.animation();
        LAYOUT_DEFAULTS.put(key, new TypeLayoutDefaults(p, a));
    }

    /**
     * 同时指定布局与客户端同步用默认展示方式
     */
    public static void register(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation,
                                EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        register(typeId, defaultPosition, defaultAnimation);
        if (clientDefaultDisplayIfAbsent != null) {
            SYNC_CLIENT_DISPLAY_IF_ABSENT.put(NotificationTypeKeys.normalizeOrDefault(typeId), clientDefaultDisplayIfAbsent);
        }
    }

    public static void ensureKnown(String typeId) {
        KNOWN.add(NotificationTypeKeys.normalizeOrDefault(typeId));
    }

    /**
     * 注销通知类型及其默认值；内置默认类型始终保留。
     */
    public static boolean unregister(String typeId) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        if (NotificationTypeKeys.DEFAULT.equals(key)) {
            return false;
        }
        boolean removed = KNOWN.remove(key);
        LAYOUT_DEFAULTS.remove(key);
        SYNC_CLIENT_DISPLAY_IF_ABSENT.remove(key);
        return removed;
    }

    /**
     * 未单独 {@link #register(String, EnumPosition, EnumMoveType)} 时返回 {@link EnumPosition#TOP_RIGHT}
     */
    public static EnumPosition defaultPosition(String typeId) {
        TypeLayoutDefaults d = LAYOUT_DEFAULTS.get(NotificationTypeKeys.normalizeOrDefault(typeId));
        return d != null ? d.position() : FALLBACK_LAYOUT.position();
    }

    /**
     * 未单独注册布局时返回 {@link EnumMoveType#AUTO}
     */
    public static EnumMoveType defaultAnimation(String typeId) {
        TypeLayoutDefaults d = LAYOUT_DEFAULTS.get(NotificationTypeKeys.normalizeOrDefault(typeId));
        return d != null ? d.animation() : FALLBACK_LAYOUT.animation();
    }

    /**
     * 发往客户端的排序副本
     */
    public static List<String> sortedSnapshot() {
        List<String> list = new ArrayList<>(KNOWN);
        Collections.sort(list);
        return list;
    }

    /**
     * 构建登录同步条目（类型 id + 可选的客户端默认展示方式）
     */
    public static List<NotificationTypeSyncEntry> buildSyncEntries() {
        List<String> ids = sortedSnapshot();
        List<NotificationTypeSyncEntry> out = new ArrayList<>(ids.size());
        for (String id : ids) {
            String key = NotificationTypeKeys.normalizeOrDefault(id);
            EnumNotificationTypeDisplayMode d = SYNC_CLIENT_DISPLAY_IF_ABSENT.get(key);
            out.add(new NotificationTypeSyncEntry(key, d));
        }
        return out;
    }

    /**
     * 某类型的默认位置与动画
     */
    public static final class TypeLayoutDefaults {
        private final EnumPosition position;
        private final EnumMoveType animation;

        public TypeLayoutDefaults(EnumPosition position, EnumMoveType animation) {
            this.position = position;
            this.animation = animation;
        }

        public EnumPosition position() {
            return position;
        }

        public EnumMoveType animation() {
            return animation;
        }
    }
}
