package xin.vanilla.banira.internal.client.ui.page;

import xin.vanilla.banira.api.client.ui.UiGameElement;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.banira.client.gui.quickaction.QuickActionEntry;
import xin.vanilla.banira.client.gui.quickaction.QuickIcon;

import java.util.List;

final class UiModuleHeaderIcons {
    private UiModuleHeaderIcons() {
    }

    static UiGameElement resolve(String namespace, List<QuickActionEntry> entries) {
        if (namespace == null || namespace.isEmpty()) return null;
        for (QuickActionEntry entry : entries) {
            if (entry == null || entry.id() == null || !entry.id().startsWith(namespace + ":")
                    || entry.quickIcon() == null
                    || entry.quickIcon().kind() != QuickIcon.Kind.RESOURCE) continue;
            Texture source = entry.quickIcon().texture();
            if (source == null) continue;
            return UiGameElement.sprite(Texture.of(source.location(), source.uvWidth(), source.uvHeight())
                    .u0(source.u0()).v0(source.v0()).uWidth(source.uWidth()).vHeight(source.vHeight()));
        }
        return null;
    }
}
