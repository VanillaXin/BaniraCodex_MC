package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraPlatforms;

import javax.annotation.Nullable;

/**
 * Banira-owned screen operations that still need native Minecraft screen types.
 */
public final class BaniraClientScreenService {

    private BaniraClientScreenService() {
    }

    /**
     * Opens Banira's config editor from client code while keeping Minecraft access in one place.
     */
    public static void openConfigEditor(ConfigHolder holder, @Nullable Screen parent) {
        if (holder != null && BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient()) {
            Minecraft.getInstance().setScreen(new ConfigEditorScreen(holder, new ConfigEditorScreen.Args().parentScreen(parent)));
        }
    }

    /**
     * Refreshes the active config editor if it is editing the received config snapshot.
     */
    public static void refreshOpenConfigEditor(String configName) {
        Screen open = Minecraft.getInstance().screen;
        if (open instanceof ConfigEditorScreen) {
            ((ConfigEditorScreen) open).refreshUIFromHolderAfterRemoteFetch(configName);
        }
    }
}
