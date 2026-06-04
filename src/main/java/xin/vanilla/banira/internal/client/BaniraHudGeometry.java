package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.event.BaniraHudBounds;

/**
 * Shared HUD geometry formulas for branch adapters and synthetic hooks.
 */
public final class BaniraHudGeometry {
    private BaniraHudGeometry() {
    }

    public static BaniraHudBounds experienceBarBounds(int x, int screenHeight) {
        return BaniraHudBounds.of(x, screenHeight - 29, 182, 5);
    }

    public static BaniraHudBounds experienceTextBounds(int x, int screenHeight) {
        return BaniraHudBounds.of(x, screenHeight - 35, 182, 9);
    }
}
