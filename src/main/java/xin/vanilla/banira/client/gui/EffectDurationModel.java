package xin.vanilla.banira.client.gui;

import java.math.BigDecimal;

public final class EffectDurationModel {
    public static final int MAX_TICKS = 51_840_000;
    private EffectDurationModel() { }

    public static int parseTicks(String text) { return parseTicks(text, 0); }

    public static int parseTicks(String text, int currentTicks) {
        if (currentTicks > 0 && Integer.toString(currentTicks).equals(text)) return currentTicks;
        if (text == null || !text.matches("[0-9]{1,8}")) throw new IllegalArgumentException("Invalid effect duration");
        int ticks = Integer.parseInt(text);
        if (ticks < 1 || ticks > MAX_TICKS) throw new IllegalArgumentException("Effect duration is out of range");
        return ticks;
    }

    public static int inputDigits(int currentTicks) { return Math.max(8, Integer.toString(currentTicks).length()); }
    public static String ticksText(int ticks) { return ticks + "t"; }
    public static String nominalSeconds(int ticks) {
        return BigDecimal.valueOf(ticks).divide(BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString();
    }
    public static float scaleToFit(int textWidth, int iconWidth) {
        return Math.min(0.75f, (float) Math.max(1, iconWidth - 2) / Math.max(1, textWidth));
    }
}
