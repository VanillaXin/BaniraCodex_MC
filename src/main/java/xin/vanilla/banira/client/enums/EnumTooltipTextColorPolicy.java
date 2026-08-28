package xin.vanilla.banira.client.enums;

/**
 * Tooltip 显式中性色的替换规则。其他颜色只按背景对比度做最小调整。
 */
public enum EnumTooltipTextColorPolicy {
    PRESERVE(false, false),
    REPLACE_WHITE(true, false),
    REPLACE_BLACK(false, true),
    REPLACE_BLACK_AND_WHITE(true, true);

    private final boolean replaceWhite;
    private final boolean replaceBlack;

    EnumTooltipTextColorPolicy(boolean replaceWhite, boolean replaceBlack) {
        this.replaceWhite = replaceWhite;
        this.replaceBlack = replaceBlack;
    }

    public boolean shouldReplace(int rgb) {
        int normalized = rgb & 0x00FFFFFF;
        return (replaceWhite && normalized == 0xFFFFFF)
                || (replaceBlack && normalized == 0x000000);
    }
}
