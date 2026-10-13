package xin.vanilla.banira.api.client.hud;

import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;

public final class BaniraMinimapLines {
    private BaniraMinimapLines() {
    }

    public static boolean register(MinimapLine line) {
        return MinimapLineRegistry.shared().register(line);
    }
}
