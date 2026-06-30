package xin.vanilla.banira.api.client.hud;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * HUD 元素推荐绘制区域，未知元素会返回 empty。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraHudBounds {
    private static final BaniraHudBounds EMPTY = new BaniraHudBounds(0, 0, 0, 0, false);

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final boolean known;

    private BaniraHudBounds(int x, int y, int width, int height, boolean known) {
        this.x = x;
        this.y = y;
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
        this.known = known;
    }

    public static BaniraHudBounds empty() {
        return EMPTY;
    }

    public static BaniraHudBounds of(int x, int y, int width, int height) {
        return new BaniraHudBounds(x, y, width, height, true);
    }

    public boolean isKnown() {
        return known;
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public int progressX(float progress) {
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        return x + Math.round(width * clamped);
    }

    public BaniraHudBounds offset(int dx, int dy) {
        return known ? of(x + dx, y + dy, width, height) : this;
    }

    public BaniraHudBounds inflate(int amount) {
        return known ? of(x - amount, y - amount, width + amount * 2, height + amount * 2) : this;
    }
}
