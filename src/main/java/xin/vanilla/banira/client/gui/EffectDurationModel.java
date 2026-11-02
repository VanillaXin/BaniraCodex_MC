package xin.vanilla.banira.client.gui;

import java.math.BigDecimal;
import java.util.Locale;

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

    public static String clockText(int ticks) {
        if (ticks == -1) return "\u221e";
        long seconds = Math.max(0, ticks) / 20L;
        String time = String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
        int fraction = Math.max(0, ticks) % 20 * 5;
        return fraction == 0 ? time : time + String.format(Locale.ROOT, ".%02d", fraction).replaceAll("0$", "");
    }

    public static int parseTime(String text, int currentTicks) {
        if ((currentTicks > 0 || currentTicks == -1) && clockText(currentTicks).equals(text)) return currentTicks;
        if (text == null || text.length() > 16 || !text.matches("[0-9]+(?::[0-9]{1,2}){0,2}(?:\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Invalid effect duration");
        String[] parts = text.split(":");
        try {
            BigDecimal seconds = BigDecimal.ZERO;
            for (int index = 0; index < parts.length; index++) {
                BigDecimal part = new BigDecimal(parts[index]);
                if (index > 0 && part.compareTo(BigDecimal.valueOf(60)) >= 0)
                    throw new IllegalArgumentException("Invalid clock component");
                seconds = seconds.multiply(BigDecimal.valueOf(60)).add(part);
            }
            int ticks = seconds.multiply(BigDecimal.valueOf(20)).intValueExact();
            if (ticks < 1 || ticks > MAX_TICKS) throw new IllegalArgumentException("Effect duration is out of range");
            return ticks;
        } catch (ArithmeticException invalid) {
            throw new IllegalArgumentException("Effect duration is out of range or precision", invalid);
        }
    }
    public static float scaleToFit(int textWidth, int iconWidth) {
        return Math.min(0.75f, (float) Math.max(1, iconWidth - 2) / Math.max(1, textWidth));
    }
}
