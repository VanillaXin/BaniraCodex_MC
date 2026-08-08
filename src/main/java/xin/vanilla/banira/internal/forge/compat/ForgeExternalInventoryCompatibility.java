package xin.vanilla.banira.internal.forge.compat;

import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** 仅在对应模组存在时装载具体兼容类，保持全部集成为可选依赖。 */
public final class ForgeExternalInventoryCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();

    private ForgeExternalInventoryCompatibility() {
    }

    public static void init() {
        ModList mods = ModList.get();
        initialize(mods, "ftblibrary",
                "xin.vanilla.banira.internal.forge.compat.ftblibrary.FtbLibraryCompatibility");
        initialize(mods, "jei",
                "xin.vanilla.banira.internal.forge.compat.jei.JeiCompatibility");
        initialize(mods, "inventoryprofilesnext",
                "xin.vanilla.banira.internal.forge.compat.ipn.InventoryProfilesNextCompatibility");
        // 各兼容桥注册完成后再统一刷新，避免首个背包界面仍沿用初始化前的宿主状态。
        xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager.get()
                .refreshCurrentScreen();
    }

    private static void initialize(ModList mods, String modId, String className) {
        if (!mods.isLoaded(modId)) return;
        try {
            Class.forName(className, true, ForgeExternalInventoryCompatibility.class.getClassLoader())
                    .getMethod("init").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Failed to initialize external inventory button compatibility for {}",
                    modId, exception);
        }
    }
}
