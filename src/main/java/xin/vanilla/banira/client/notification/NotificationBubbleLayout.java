package xin.vanilla.banira.client.notification;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.util.List;

public final class NotificationBubbleLayout {
    public static final int BODY_TOP = 18, BOTTOM = 4, LINE_HEIGHT = 10;
    private static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");
    private NotificationBubbleLayout() { }
    public static int lineLimit(int height) { return Math.max(1, (height - BODY_TOP - BOTTOM) / LINE_HEIGHT); }
    public static int height(int lines) { return BODY_TOP + BOTTOM + Math.max(1, lines) * LINE_HEIGHT; }
    public static boolean supports(List<FormattedCharSequence> lines) {
        return lines.stream().allMatch(line -> line.accept((index, style, codePoint) -> !style.isObfuscated()
                && !style.isStrikethrough() && DEFAULT_FONT.equals(style.getFont())));
    }
}
