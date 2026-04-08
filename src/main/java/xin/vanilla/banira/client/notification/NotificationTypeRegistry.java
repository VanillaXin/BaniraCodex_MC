package xin.vanilla.banira.client.notification;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端已知的通知类型集合。默认包含 {@link NotificationTypeKeys#DEFAULT}，收到通知或加载配置时会自动登记。
 * <p>
 * <b>依赖 Mod 推荐用法</b>：在本 Mod 的 {@link net.neoforged.fml.event.lifecycle.FMLClientSetupEvent}（或同阶段）中一次性调用
 * {@link #register(String)} / {@link #register(String, EnumNotificationTypeDisplayMode)} 登记本 Mod 会收到的全部类型 id。
 * {@link NotificationTypeSettingsStore#load()} 执行完毕后会根据登记项，对「JSON 中尚不存在」的类型写入默认 {@code displayMode}，不会覆盖玩家已有配置。
 * 登录时服务端还会通过 {@link xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient} 下发类型列表及可选展示默认值，
 * 客户端无需再维护与服务端完全一致的硬编码列表；若你在本机 {@code register(id, mode)} 过，则优先于服务端建议。
 */
@OnlyIn(Dist.CLIENT)
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
        String t = NotificationTypeKeys.normalizeOrDefault(typeId);
        KNOWN.add(t);
        if (defaultIfAbsent != null) {
            MOD_REGISTERED_DISPLAY_DEFAULT.put(t, defaultIfAbsent);
        } else {
            MOD_REGISTERED_DISPLAY_DEFAULT.remove(t);
        }
        if (NotificationTypeSettingsStore.get().isSettingsLoadedFromDisk()) {
            NotificationTypeSettingsStore.get().applyResolvedDisplayDefaultIfNoSavedEntry(t);
        }
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
