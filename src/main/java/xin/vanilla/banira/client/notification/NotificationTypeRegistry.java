package xin.vanilla.banira.client.notification;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端已知的通知类型集合。默认包含 {@link NotificationTypeKeys#DEFAULT}，收到通知或加载配置时会自动登记。
 */
@OnlyIn(Dist.CLIENT)
public final class NotificationTypeRegistry {

    private static final Set<String> KNOWN = ConcurrentHashMap.newKeySet();

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

    public static void ensureKnown(String typeId) {
        KNOWN.add(NotificationTypeKeys.normalizeOrDefault(typeId));
    }

    /**
     * 合并服务端在玩家登录时同步的类型 id
     */
    public static void registerAllFromServer(Iterable<String> typeIds) {
        if (typeIds == null) {
            return;
        }
        for (String id : typeIds) {
            if (id != null) {
                KNOWN.add(NotificationTypeKeys.normalizeOrDefault(id));
            }
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
