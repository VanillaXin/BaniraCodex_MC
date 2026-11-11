package xin.vanilla.banira.client.notification;

import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class NotificationBubbleLayout {
    public static final int BODY_TOP = 18, BOTTOM = 4, LINE_HEIGHT = 10;

    private NotificationBubbleLayout() {
    }

    public static int maximumWidth(int screenWidth, int regionWidth) {
        int preferred = 180 + (int) Math.round(Math.max(0, screenWidth - 320) * 0.25);
        return Math.max(1, Math.min(regionWidth, Math.min(420, preferred)));
    }

    public static int width(int screenWidth, int regionWidth, int contentWidth) {
        int minimum = Math.min(240, 120 + (int) Math.round(Math.max(0, screenWidth - 320) * 0.16));
        return Math.min(maximumWidth(screenWidth, regionWidth), Math.max(minimum, contentWidth));
    }

    public static int lineLimit(int height) {
        return Math.max(1, (height - BODY_TOP - BOTTOM) / LINE_HEIGHT);
    }

    public static int height(int lines) {
        return BODY_TOP + BOTTOM + Math.max(1, lines) * LINE_HEIGHT;
    }

    public static boolean supports(List<FormattedCharSequence> lines) {
        return lines.stream().allMatch(line -> line.accept((index, style, codePoint) -> !style.isObfuscated()
                && !style.isStrikethrough() && Style.EMPTY.getFont().equals(style.getFont())));
    }
}
