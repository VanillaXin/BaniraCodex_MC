package xin.vanilla.banira.internal.client.ui.page;

import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiPageSource;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.ScopedComponent;

final class UiCommonChrome {
    private UiCommonChrome() {
    }

    static void bind(UiView view, Component title, Screen parent) {
        String namespace = title == null ? null : title.modId();
        if ((namespace == null || namespace.isEmpty()) && parent instanceof UiPageSource) {
            String surface = ((UiPageSource) parent).uiSurfaceId();
            if (surface != null && surface.indexOf(':') > 0) namespace = surface.substring(0, surface.indexOf(':'));
        }
        if (namespace == null || namespace.isEmpty()) namespace = "banira_codex";
        var icon = UiModuleHeaderIcons.resolve(namespace, QuickActionRegistry.get().allEntriesInOrder());
        view.gameElement("brand", icon);
        view.enabled("brand", icon != null);
        view.text("module_name", new ScopedComponent(namespace).transClientAuto("mod_name"));
        view.tooltip("title", title == null ? BaniraComponent.get().empty() : title);
    }
}
