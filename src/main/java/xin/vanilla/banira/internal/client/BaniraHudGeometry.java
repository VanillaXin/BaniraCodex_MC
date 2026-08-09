package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.api.client.hud.BaniraHudBounds;

/**
 * 原版 HUD 常见元素位置公式，供不同加载器 adapter 复用。
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
