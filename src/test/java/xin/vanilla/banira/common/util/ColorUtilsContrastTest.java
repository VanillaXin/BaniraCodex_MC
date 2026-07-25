package xin.vanilla.banira.common.util;

import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 验证通知等浅色浮层上的文字始终保有足够对比度。 */
public class ColorUtilsContrastTest {
    @Test
    public void yellowTextIsDarkenedWithoutLosingItsHue() {
        int result = ColorUtils.ensureReadableTextArgb(0xFFFFD700, 0xFFFFF4C2);

        assertNotEquals(0xFF000000, result);
        assertNotEquals(0xFFFFFFFF, result);
        assertTrue(ColorUtils.contrastRatio(result, 0xFFFFF4C2) >= 4.5);
        assertTrue(((result >> 16) & 0xFF) > ((result >> 8) & 0xFF));
        assertTrue(((result >> 8) & 0xFF) > (result & 0xFF));
    }

    @Test
    public void readableSemanticColorIsPreserved() {
        int result = ColorUtils.ensureReadableTextArgb(0xFF7A1F1F, 0xFFFFE5E5);

        assertEquals(0xFF7A1F1F, result);
    }

    @Test
    public void embeddedYellowFormattingKeepsAChromaticLegacyColor() {
        String result = ColorUtils.ensureReadableMinecraftFormatting(
                "\u00A7r\u00A7eCountdown: 3", 0xFFFFF4C2);

        assertNotEquals("\u00A7r\u00A70Countdown: 3", result);
        assertNotEquals("\u00A7r\u00A7fCountdown: 3", result);
        assertNotEquals("\u00A7r\u00A7eCountdown: 3", result);
    }

    @Test
    public void readableComponentCopyLeavesLegacyCodesForPostTranslationProcessing() {
        String source = "\u00A7eCountdown: 3";
        Component component = BaniraComponent.get().literal(source);

        Component readable = ColorUtils.readableComponentCopy(component, 0xFFFFF4C2);

        assertEquals("\u00A7eCountdown: 3", source);
        assertEquals("\u00A7eCountdown: 3", component.text());
        assertEquals(component.text(), readable.text());
    }

    @Test
    public void postTranslationLegacyYellowBecomesReadableDarkYellow() {
        net.minecraft.network.chat.Component source =
                new net.minecraft.network.chat.TextComponent("\u00A7eCountdown: 3");

        net.minecraft.network.chat.Component readable =
                ColorUtils.readableVanillaComponentCopy(source, 0xFFFFF4C2);

        assertEquals("Countdown: 3", readable.getString());
        assertTrue(!readable.getSiblings().isEmpty());
        net.minecraft.network.chat.TextColor color = readable.getSiblings().get(0).getStyle().getColor();
        assertNotNull(color);
        int rgb = color.getValue();
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        assertTrue(Math.abs(red - green) <= 2);
        assertTrue(green > blue);
        assertTrue(ColorUtils.contrastRatio(0xFF000000 | rgb, 0xFFFFF4C2) >= 4.5);
    }

    @Test
    public void readableComponentCopyDoesNotMutateStoredNotificationColor() {
        Component source = BaniraComponent.get().literal("Countdown").color(Color.argb(0xFFFFD700));

        Component readable = ColorUtils.readableComponentCopy(source, 0xFFFFF4C2);

        assertEquals(0xFFFFD700, source.color().argb());
        assertNotEquals(source.color().argb(), readable.color().argb());
        assertTrue(ColorUtils.contrastRatio(readable.color().argb(), 0xFFFFF4C2) >= 4.5);
    }
}
