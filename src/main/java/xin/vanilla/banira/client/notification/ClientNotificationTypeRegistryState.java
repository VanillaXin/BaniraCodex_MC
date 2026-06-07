package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * 客户端通知类型注册状态；只保存类型与默认展示方式，不直接读写配置文件。
 */
public final class ClientNotificationTypeRegistryState {

    private final Object lock = new Object();
    private final Set<String> known = new LinkedHashSet<>();
    private final Map<String, EnumNotificationTypeDisplayMode> modRegisteredDisplayDefault = new LinkedHashMap<>();
    private final Map<String, EnumNotificationTypeDisplayMode> serverSyncedDisplayDefault = new LinkedHashMap<>();

    public ClientNotificationTypeRegistryState() {
        known.add(NotificationTypeKeys.DEFAULT);
    }

    public String register(String typeId) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        synchronized (lock) {
            known.add(key);
        }
        return key;
    }

    public String register(String typeId, EnumNotificationTypeDisplayMode defaultIfAbsent) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        synchronized (lock) {
            known.add(key);
            if (defaultIfAbsent != null) {
                modRegisteredDisplayDefault.put(key, defaultIfAbsent);
            } else {
                modRegisteredDisplayDefault.remove(key);
            }
        }
        return key;
    }

    public void registerAllFromServer(Iterable<String> typeIds) {
        if (typeIds == null) {
            return;
        }
        synchronized (lock) {
            for (String id : typeIds) {
                if (id != null) {
                    known.add(NotificationTypeKeys.normalizeOrDefault(id));
                }
            }
        }
    }

    public void acceptServerSyncedDisplayDefault(String typeId, EnumNotificationTypeDisplayMode mode) {
        if (mode == null) {
            return;
        }
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        synchronized (lock) {
            known.add(key);
            if (!modRegisteredDisplayDefault.containsKey(key)) {
                serverSyncedDisplayDefault.put(key, mode);
            }
        }
    }

    public EnumNotificationTypeDisplayMode resolvedDisplayDefault(String typeId) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        synchronized (lock) {
            EnumNotificationTypeDisplayMode mode = modRegisteredDisplayDefault.get(key);
            return mode != null ? mode : serverSyncedDisplayDefault.get(key);
        }
    }

    @Nonnull
    public Set<String> typeIdsWithResolvedDefaults() {
        synchronized (lock) {
            Set<String> union = new LinkedHashSet<>(modRegisteredDisplayDefault.keySet());
            union.addAll(serverSyncedDisplayDefault.keySet());
            return Collections.unmodifiableSet(union);
        }
    }

    @Nonnull
    public List<String> knownTypesSorted(Collection<String> storedTypeIds) {
        synchronized (lock) {
            Set<String> all = new LinkedHashSet<>(known);
            if (storedTypeIds != null) {
                for (String id : storedTypeIds) {
                    all.add(NotificationTypeKeys.normalizeOrDefault(id));
                }
            }
            List<String> out = new ArrayList<>(all);
            Collections.sort(out);
            return Collections.unmodifiableList(out);
        }
    }

    public void clear() {
        synchronized (lock) {
            known.clear();
            modRegisteredDisplayDefault.clear();
            serverSyncedDisplayDefault.clear();
            known.add(NotificationTypeKeys.DEFAULT);
        }
    }
}
