package xin.vanilla.banira.internal.fabric.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.internal.compat.FtbLibraryContract;

/**
 * 在可选模组存在时安装对应的 Fabric 背包按钮桥。
 */
public final class FabricExternalInventoryCompatibility {
    private static final Logger LOGGER = LogManager.getLogger();

    private FabricExternalInventoryCompatibility() {
    }

    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("ftblibrary") && !FtbLibraryContract.matches()) {
            LOGGER.error("FTB Library compatibility disabled; native controls remain active: {}",
                    FtbLibraryContract.mismatch());
        } else if (FabricLoader.getInstance().isModLoaded("ftblibrary")) {
            try {
                Class.forName("xin.vanilla.banira.internal.fabric.compat.ftblibrary.FtbLibraryCompatibility")
                        .getMethod("init").invoke(null);
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.error("Failed to initialize FTB Library inventory button compatibility", exception);
            }
        }
        ExternalInventoryButtonManager.get().refreshCurrentScreen();
    }
}
