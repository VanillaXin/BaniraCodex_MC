package xin.vanilla.banira.internal.neoforge.compat;

import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 仅在对应模组存在时装载具体兼容类，保持全部集成为可选依赖。 */
public final class NeoForgeExternalInventoryCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();

    private NeoForgeExternalInventoryCompatibility() {
    }

    public static void init() {
        ModList mods = ModList.get();
        initialize(mods, "jei",
                "xin.vanilla.banira.internal.neoforge.compat.jei.JeiCompatibility");
        // 各兼容桥注册完成后再统一刷新，避免首个背包界面仍沿用初始化前的宿主状态。
        xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager.get()
                .refreshCurrentScreen();
    }

    private static void initialize(ModList mods, String modId, String className) {
        if (!mods.isLoaded(modId)) return;
        try {
            Class.forName(className, true, NeoForgeExternalInventoryCompatibility.class.getClassLoader())
                    .getMethod("init").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Failed to initialize external inventory button compatibility for {}",
                    modId, exception);
        }
    }
}
