package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side notification type registry.
 * <p>
 * Dependent mods should register their notification type ids during client setup,
 * for example from {@link BaniraClientEventHub.ModLifecycle#onClientSetup(Runnable)}.
 * Local registrations take priority over server-synced display defaults.
 */
public final class NotificationTypeRegistry {

    private static final Set<String> KNOWN = ConcurrentHashMap.newKeySet();
    /**
     * 本 Mod 在客户端显式登记的「配置文件无条目时」展示方式
     */
    private static final ConcurrentHashMap<String, EnumNotificationTypeDisplayMode> MOD_REGISTERED_DISPLAY_DEFAULT = new ConcurrentHashMap<>();
    /**
     * 登录包下发的展示方式建议（不与本 Mod 显式登记冲突）
     */
    private static final ConcurrentHashMap<String, EnumNotificationTypeDisplayMode> SERVER_SYNCED_DISPLAY_DEFAULT = new ConcurrentHashMap<>();
    /**
     * 子 Mod 提供的本地化类型说明，仅用于客户端配置界面。
     */
    private static final ConcurrentHashMap<String, Component> TYPE_TOOLTIPS = new ConcurrentHashMap<>();
    /**
     * 子 Mod 提供的本地化名称，作为通知类型树的根分组标题。
     */
    private static final ConcurrentHashMap<String, Component> MOD_DISPLAY_NAMES = new ConcurrentHashMap<>();

    static {
        KNOWN.add(NotificationTypeKeys.DEFAULT);
    }

    private NotificationTypeRegistry() {
    }

    /**
     * 显式注册类型（可在客户端 Mod 初始化时调用，便于配置界面提前列出）。
     */
    public static void register(String typeId) {
        KNOWN.add(NotificationTypeKeys.normalizeOrDefault(typeId));
    }

    /**
     * 显式注册类型，并指定：当 {@link NotificationTypeSettingsStore} 已加载的 JSON 中<strong>不存在</strong>该类型条目时使用的默认 {@code displayMode}。
     * 不会覆盖 JSON 中已有条目。若在 {@link NotificationTypeSettingsStore#load()} 之后调用，则立即对「当前内存中无该键」的情况补写并异步保存。
     */
    public static void register(String typeId, EnumNotificationTypeDisplayMode defaultIfAbsent) {
        register(typeId, defaultIfAbsent, null);
    }

    /**
     * 显式注册类型、默认展示方式和配置界面说明。
     */
    public static void register(String typeId, EnumNotificationTypeDisplayMode defaultIfAbsent,
                                @Nullable Component tooltip) {
        String t = NotificationTypeKeys.normalizeOrDefault(typeId);
        KNOWN.add(t);
        if (defaultIfAbsent != null) {
            MOD_REGISTERED_DISPLAY_DEFAULT.put(t, defaultIfAbsent);
        } else {
            MOD_REGISTERED_DISPLAY_DEFAULT.remove(t);
        }
        if (tooltip != null && !tooltip.isEmpty()) {
            TYPE_TOOLTIPS.put(t, tooltip.clone());
        } else {
            TYPE_TOOLTIPS.remove(t);
        }
        if (NotificationTypeSettingsStore.get().isSettingsLoadedFromDisk()) {
            NotificationTypeSettingsStore.get().applyResolvedDisplayDefaultIfNoSavedEntry(t);
        }
    }

    public static void register(String typeId, @Nullable Component tooltip) {
        register(typeId, null, tooltip);
    }

    /**
     * 登记通知类型树中 modId 根分组使用的本地化名称。
     */
    public static void registerModDisplayName(String modId, @Nullable Component displayName) {
        if (modId == null || modId.trim().isEmpty()) {
            return;
        }
        String normalized = modId.trim().toLowerCase(Locale.ROOT);
        if (displayName != null && !displayName.isEmpty()) {
            MOD_DISPLAY_NAMES.put(normalized, displayName.clone());
        } else {
            MOD_DISPLAY_NAMES.remove(normalized);
        }
    }

    @Nullable
    public static Component tooltip(String typeId) {
        Component tooltip = TYPE_TOOLTIPS.get(NotificationTypeKeys.normalizeOrDefault(typeId));
        return tooltip != null ? tooltip.clone() : null;
    }

    @Nullable
    public static Component modDisplayName(String modId) {
        if (modId == null) {
            return null;
        }
        Component displayName = MOD_DISPLAY_NAMES.get(modId.trim().toLowerCase(Locale.ROOT));
        return displayName != null ? displayName.clone() : null;
    }

    public static void ensureKnown(String typeId) {
        KNOWN.add(NotificationTypeKeys.normalizeOrDefault(typeId));
    }

    /**
     * 合并服务端在玩家登录时同步的类型 id（无展示方式字段时的兼容用法）
     */
    public static void registerAllFromServer(Iterable<String> typeIds) {
        if (typeIds == null) {
            return;
        }
        for (String id : typeIds) {
            if (id != null) {
                ensureKnown(id);
            }
        }
    }

    /**
     * 接收登录同步包中的展示方式建议（若本 Mod 已通过 {@link #register(String, EnumNotificationTypeDisplayMode)} 登记过该 id，则忽略服务端值）。
     */
    public static void acceptServerSyncedDisplayDefault(String typeId, EnumNotificationTypeDisplayMode mode) {
        String t = NotificationTypeKeys.normalizeOrDefault(typeId);
        if (mode == null) {
            return;
        }
        if (MOD_REGISTERED_DISPLAY_DEFAULT.containsKey(t)) {
            return;
        }
        SERVER_SYNCED_DISPLAY_DEFAULT.put(t, mode);
    }

    /**
     * 本 Mod 登记优先，否则为登录同步建议
     */
    public static EnumNotificationTypeDisplayMode resolvedDisplayDefault(String typeId) {
        String t = NotificationTypeKeys.normalizeOrDefault(typeId);
        EnumNotificationTypeDisplayMode m = MOD_REGISTERED_DISPLAY_DEFAULT.get(t);
        if (m != null) {
            return m;
        }
        return SERVER_SYNCED_DISPLAY_DEFAULT.get(t);
    }

    /**
     * 在 {@link NotificationTypeSettingsStore#load()} 完成后调用：对存在解析后默认、且 JSON 未包含条目的类型写入 {@link NotificationTypeSettingsStore}
     */
    public static void applyAllResolvedDefaultsAfterStoreLoad() {
        Set<String> union = new HashSet<>(MOD_REGISTERED_DISPLAY_DEFAULT.keySet());
        union.addAll(SERVER_SYNCED_DISPLAY_DEFAULT.keySet());
        for (String id : union) {
            NotificationTypeSettingsStore.get().applyResolvedDisplayDefaultIfNoSavedEntry(id);
        }
    }

    public static List<String> knownTypesSorted() {
        Set<String> fromSettings = NotificationTypeSettingsStore.get().typeIdsFromStored();
        List<String> all = new ArrayList<>(KNOWN);
        for (String s : fromSettings) {
            if (!all.contains(s)) {
                all.add(s);
            }
        }
        Collections.sort(all);
        return all;
    }
}
