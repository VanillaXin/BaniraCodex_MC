package xin.vanilla.banira.client;

import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorThemeLoader;
import xin.vanilla.banira.client.event.BaniraTextureReloadEvent;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.TextureUtils;

/**
 * Banira-owned client resource behavior. Loader adapters provide the resource
 * hooks; this service keeps Codex reload logic out of those adapters.
 */
public final class BaniraClientResourceService {

    private BaniraClientResourceService() {
    }

    public static BaniraColorThemeLoader colorThemeLoader() {
        return BaniraColorThemeLoader.get();
    }

    public static void handleTextureReload(BaniraTextureReloadEvent event) {
        if (event == null || event.atlasLocation() == null) {
            return;
        }
        if (BaniraCodex.MODID.equals(event.atlasLocation().getNamespace())) {
            TextureUtils.resourceReloadEvent();
            QuickActionOverlay.resetSystemIconTextureCache();
        }
    }
}
