package xin.vanilla.banira.internal.fabric.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;

/** 仅在对应模组存在时装载 Fabric 兼容类，保持全部集成为可选依赖。 */
public final class FabricExternalInventoryCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();

    private FabricExternalInventoryCompatibility() {
    }

    public static void init() {
        FabricLoader loader = FabricLoader.getInstance();
        initialize(loader, "ftblibrary",
                "xin.vanilla.banira.internal.fabric.compat.ftblibrary.FtbLibraryCompatibility");
        initialize(loader, "jei",
                "xin.vanilla.banira.internal.fabric.compat.jei.JeiCompatibility");
        ExternalInventoryButtonManager.get().refreshCurrentScreen();
    }

    private static void initialize(FabricLoader loader, String modId, String className) {
        if (!loader.isModLoaded(modId)) return;
        try {
            Class.forName(className, true, FabricExternalInventoryCompatibility.class.getClassLoader())
                    .getMethod("init").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.error("Failed to initialize external inventory button compatibility for {}",
                    modId, exception);
        }
    }
}
