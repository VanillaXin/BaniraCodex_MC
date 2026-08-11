package xin.vanilla.banira.api.client;

import xin.vanilla.banira.api.quickaction.CustomQuickActionDefinition;
import xin.vanilla.banira.client.gui.quickaction.CustomQuickActionManager;

import javax.annotation.Nonnull;
import java.util.List;

/** 玩家自定义快捷入口的公共客户端入口。 */
public final class BaniraQuickActions {
    private BaniraQuickActions() {
    }

    public static List<CustomQuickActionDefinition> definitions() {
        return CustomQuickActionManager.get().definitions();
    }

    public static void replaceDefinitions(List<CustomQuickActionDefinition> definitions) {
        CustomQuickActionManager.get().replaceDefinitions(definitions);
    }

    public static void reload() {
        CustomQuickActionManager.get().reload();
    }

    /** 注册可被自定义入口选择的窗口工厂，避免必须依赖反射构造器。 */
    public static void registerScreen(@Nonnull String id, @Nonnull BaniraQuickActionScreenFactory factory) {
        CustomQuickActionManager.get().registerScreen(id, factory);
    }
}
