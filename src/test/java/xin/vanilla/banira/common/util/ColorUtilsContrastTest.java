package xin.vanilla.banira.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** 验证通知等浅色浮层上的文字始终保有足够对比度。 */
public class ColorUtilsContrastTest {
    @Test
    public void yellowTextFallsBackToBlackOnPaleYellowBackground() {
        int result = ColorUtils.ensureReadableTextArgb(0xFFFFD700, 0xFFFFF4C2);

        assertEquals(0xFF000000, result);
    }

    @Test
    public void readableSemanticColorIsPreserved() {
        int result = ColorUtils.ensureReadableTextArgb(0xFF7A1F1F, 0xFFFFE5E5);

        assertEquals(0xFF7A1F1F, result);
    }

    @Test
    public void embeddedYellowFormattingIsRewrittenForPaleBackground() {
        String result = ColorUtils.ensureReadableMinecraftFormatting(
                "\u00A7r\u00A7eCountdown: 3", 0xFFFFF4C2);

        assertEquals("\u00A7r\u00A70Countdown: 3", result);
    }
}
