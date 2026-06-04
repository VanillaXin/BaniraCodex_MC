package xin.vanilla.banira.api.client.hud;

/**
 * Banira 自己的 HUD 元素枚举；加载器事件名只在 adapter 内转换。
 */
public enum HudOverlayElement {
    ALL,
    HOTBAR,
    EXPERIENCE,
    EXPERIENCE_BAR,
    EXPERIENCE_TEXT,
    HEALTH,
    ARMOR,
    FOOD,
    AIR,
    CHAT,
    CROSSHAIR,
    BOSS_HEALTH,
    PLAYER_LIST,
    DEBUG_TEXT,
    TEXT,
    UNKNOWN;

    public boolean isExperience() {
        return this == EXPERIENCE || this == EXPERIENCE_BAR || this == EXPERIENCE_TEXT;
    }
}
