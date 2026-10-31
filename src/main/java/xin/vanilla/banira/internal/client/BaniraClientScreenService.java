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
    public static void applyRemoteConfigSnapshot(String configName, java.util.Map<String, Object> snapshot) {
        xin.vanilla.banira.api.client.ui.UiPageSpec page = BaniraCodexClientBootstrap.uiSessions().page();
        if (page != null && page.session() instanceof xin.vanilla.banira.internal.client.ui.page.ConfigUiSession) {
            ((xin.vanilla.banira.internal.client.ui.page.ConfigUiSession) page.session()).acceptRemoteSnapshot(configName, snapshot);
            return;
        }
        Screen open = Minecraft.getInstance().screen;
        if (open instanceof ConfigEditorScreen) {
            ((ConfigEditorScreen) open).applyRemoteSnapshot(configName, snapshot);
        }
    }

    private BaniraClientScreenService() {
    }

    /**
     * Opens Banira's config editor from client code while keeping Minecraft access in one place.
     */
    public static void openConfigEditor(ConfigHolder holder, @Nullable Screen parent) {
        if (holder != null && BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient()) {
            ConfigEditorScreen.Args args = new ConfigEditorScreen.Args().parentScreen(parent);
            xin.vanilla.banira.internal.client.ui.page.ConfigUiSession session =
                    new xin.vanilla.banira.internal.client.ui.page.ConfigUiSession(holder, args);
            if (BaniraCodexClientBootstrap.uiBackend().wantsAui("banira_codex:config")
                    && BaniraCodexClientBootstrap.uiSessions().open(session.spec(), parent)) return;
            Minecraft.getInstance().setScreen(new ConfigEditorScreen(holder, args, session));
        }
    }

    /**
     * Refreshes the active config editor if it is editing the received config snapshot.
     */
    public static void refreshOpenConfigEditor(String configName) {
        xin.vanilla.banira.api.client.ui.UiPageSpec page = BaniraCodexClientBootstrap.uiSessions().page();
        if (page != null && page.session() instanceof xin.vanilla.banira.internal.client.ui.page.ConfigUiSession) {
            ((xin.vanilla.banira.internal.client.ui.page.ConfigUiSession) page.session()).refresh(configName);
            return;
        }
        Screen open = Minecraft.getInstance().screen;
        if (open instanceof ConfigEditorScreen) {
            ((ConfigEditorScreen) open).refreshUIFromHolderAfterRemoteFetch(configName);
        }
    }
}
