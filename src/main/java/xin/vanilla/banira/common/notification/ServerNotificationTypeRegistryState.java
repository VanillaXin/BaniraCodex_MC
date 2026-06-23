package xin.vanilla.banira.common.notification;

import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端通知类型注册状态；调用入口保留在 {@link ServerNotificationTypeRegistry}。
 */
public final class ServerNotificationTypeRegistryState {

    private static final ServerNotificationTypeRegistry.TypeLayoutDefaults FALLBACK_LAYOUT =
            new ServerNotificationTypeRegistry.TypeLayoutDefaults(EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);

    private final Object lock = new Object();
    private final Map<String, ServerNotificationTypeRegistry.TypeLayoutDefaults> layoutDefaults = new LinkedHashMap<>();
    private final Map<String, EnumNotificationTypeDisplayMode> clientDisplayIfAbsent = new LinkedHashMap<>();

    public ServerNotificationTypeRegistryState() {
        ensureKnown(NotificationTypeKeys.DEFAULT);
    }

    public void ensureKnown(String typeId) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        synchronized (lock) {
            layoutDefaults.putIfAbsent(key, null);
        }
    }

    public void register(String typeId) {
        ensureKnown(typeId);
    }

    public void register(String typeId, EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        synchronized (lock) {
            layoutDefaults.putIfAbsent(key, null);
            if (clientDefaultDisplayIfAbsent != null) {
                clientDisplayIfAbsent.put(key, clientDefaultDisplayIfAbsent);
            }
        }
    }

    public void register(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        EnumPosition p = defaultPosition != null ? defaultPosition : FALLBACK_LAYOUT.position();
        EnumMoveType a = defaultAnimation != null ? defaultAnimation : FALLBACK_LAYOUT.animation();
        synchronized (lock) {
            layoutDefaults.put(key, new ServerNotificationTypeRegistry.TypeLayoutDefaults(p, a));
        }
    }

    public void register(String typeId, EnumPosition defaultPosition, EnumMoveType defaultAnimation,
                         EnumNotificationTypeDisplayMode clientDefaultDisplayIfAbsent) {
        register(typeId, defaultPosition, defaultAnimation);
        if (clientDefaultDisplayIfAbsent != null) {
            synchronized (lock) {
                clientDisplayIfAbsent.put(NotificationTypeKeys.normalizeOrDefault(typeId), clientDefaultDisplayIfAbsent);
            }
        }
    }

    public boolean unregister(String typeId) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        if (NotificationTypeKeys.DEFAULT.equals(key)) {
            return false;
        }
        synchronized (lock) {
            boolean removed = layoutDefaults.containsKey(key) || clientDisplayIfAbsent.containsKey(key);
            layoutDefaults.remove(key);
            clientDisplayIfAbsent.remove(key);
            return removed;
        }
    }

    @Nonnull
    public ServerNotificationTypeRegistry.TypeLayoutDefaults defaultLayout(String typeId) {
        synchronized (lock) {
            ServerNotificationTypeRegistry.TypeLayoutDefaults defaults = layoutDefaults.get(NotificationTypeKeys.normalizeOrDefault(typeId));
            return defaults != null ? defaults : FALLBACK_LAYOUT;
        }
    }

    public EnumNotificationTypeDisplayMode clientDisplayIfAbsent(String typeId) {
        synchronized (lock) {
            return clientDisplayIfAbsent.get(NotificationTypeKeys.normalizeOrDefault(typeId));
        }
    }

    @Nonnull
    public List<String> sortedSnapshot() {
        synchronized (lock) {
            List<String> list = new ArrayList<>(layoutDefaults.keySet());
            Collections.sort(list);
            return Collections.unmodifiableList(list);
        }
    }

    @Nonnull
    public List<NotificationTypeSyncEntry> buildSyncEntries() {
        List<String> ids = sortedSnapshot();
        List<NotificationTypeSyncEntry> out = new ArrayList<>(ids.size());
        synchronized (lock) {
            for (String id : ids) {
                String key = NotificationTypeKeys.normalizeOrDefault(id);
                out.add(new NotificationTypeSyncEntry(key, clientDisplayIfAbsent.get(key)));
            }
        }
        return out;
    }

    public void clear() {
        synchronized (lock) {
            layoutDefaults.clear();
            clientDisplayIfAbsent.clear();
            layoutDefaults.put(NotificationTypeKeys.DEFAULT, null);
        }
    }
}
