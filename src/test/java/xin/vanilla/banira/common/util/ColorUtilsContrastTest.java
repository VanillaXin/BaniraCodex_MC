package xin.vanilla.banira.common.util;

import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 验证通知等浅色浮层上的文字始终保有足够对比度。 */
public class ColorUtilsContrastTest {
    private static final String LEGACY_CODES = "0123456789abcdef";
    private static final int[] LEGACY_COLORS = {
            0xFF000000, 0xFF0000AA, 0xFF00AA00, 0xFF00AAAA,
            0xFFAA0000, 0xFFAA00AA, 0xFFFFAA00, 0xFFAAAAAA,
            0xFF555555, 0xFF5555FF, 0xFF55FF55, 0xFF55FFFF,
            0xFFFF5555, 0xFFFF55FF, 0xFFFFFF55, 0xFFFFFFFF
    };
    private static final int[] TEST_BACKGROUNDS = {0xFFFFF4C2, 0xFF18212B};

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
        net.minecraft.util.text.ITextComponent source =
                new net.minecraft.util.text.StringTextComponent("\u00A7eCountdown: 3");

        net.minecraft.util.text.ITextComponent readable =
                ColorUtils.readableVanillaComponentCopy(source, 0xFFFFF4C2);

        assertEquals("Countdown: 3", readable.getString());
        assertTrue(!readable.getSiblings().isEmpty());
        net.minecraft.util.text.Color color = readable.getSiblings().get(0).getStyle().getColor();
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
    public void everyLegacyColorUsesTheSameContrastRuleOnLightAndDarkBackgrounds() {
        for (int background : TEST_BACKGROUNDS) {
            for (int i = 0; i < LEGACY_CODES.length(); i++) {
                net.minecraft.util.text.ITextComponent source =
                        new net.minecraft.util.text.StringTextComponent(
                                "\u00A7" + LEGACY_CODES.charAt(i) + LEGACY_CODES.charAt(i));

                List<Integer> colors = renderedSegmentColors(
                        ColorUtils.readableVanillaComponentCopy(source, background));

                assertEquals("legacy color " + LEGACY_CODES.charAt(i), 1, colors.size());
                int expected = ColorUtils.ensureReadableTextArgb(LEGACY_COLORS[i], background);
                assertEquals("legacy color " + LEGACY_CODES.charAt(i),
                        expected & 0x00FFFFFF, colors.get(0).intValue());
                assertTrue("legacy color " + LEGACY_CODES.charAt(i),
                        ColorUtils.contrastRatio(0xFF000000 | colors.get(0), background) >= 4.5);
            }
        }
    }

    @Test
    public void explicitRgbColorsUseTheSameContrastRuleOnLightAndDarkBackgrounds() {
        int[] colors = {
                0xFFF25C54, 0xFFF2A65A, 0xFFFFE066, 0xFF5BCB77,
                0xFF2EC4B6, 0xFF4D96FF, 0xFF845EC2, 0xFFFF6FB5,
                0xFF000000, 0xFF808080, 0xFFFFFFFF
        };
        for (int background : TEST_BACKGROUNDS) {
            for (int color : colors) {
                net.minecraft.util.text.ITextComponent source =
                        new net.minecraft.util.text.StringTextComponent("rgb").setStyle(
                                net.minecraft.util.text.Style.EMPTY.withColor(
                                        net.minecraft.util.text.Color.fromRgb(color & 0x00FFFFFF)));

                List<Integer> rendered = renderedSegmentColors(
                        ColorUtils.readableVanillaComponentCopy(source, background));

                assertEquals(1, rendered.size());
                int expected = ColorUtils.ensureReadableTextArgb(color, background);
                assertEquals(expected & 0x00FFFFFF, rendered.get(0).intValue());
                assertTrue(ColorUtils.contrastRatio(0xFF000000 | rendered.get(0), background) >= 4.5);
            }
        }
    }

    @Test
    public void mixedLegacyColorsAreAdaptedPerSegmentInsteadOfUsingTheFirstColor() {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < LEGACY_CODES.length(); i++) {
            formatted.append('\u00A7').append(LEGACY_CODES.charAt(i)).append(LEGACY_CODES.charAt(i));
        }

        net.minecraft.util.text.ITextComponent readable = ColorUtils.readableVanillaComponentCopy(
                new net.minecraft.util.text.StringTextComponent(formatted.toString()), 0xFFFFF4C2);
        List<Integer> colors = renderedSegmentColors(readable);

        assertEquals(LEGACY_CODES, readable.getString());
        assertEquals(LEGACY_CODES.length(), colors.size());
        for (int i = 0; i < colors.size(); i++) {
            int expected = ColorUtils.ensureReadableTextArgb(LEGACY_COLORS[i], 0xFFFFF4C2);
            assertEquals(expected & 0x00FFFFFF, colors.get(i).intValue());
        }
    }

    @Test
    public void readableComponentCopyDoesNotMutateStoredNotificationColor() {
        Component source = BaniraComponent.get().literal("Countdown").color(Color.argb(0xFFFFD700));

        Component readable = ColorUtils.readableComponentCopy(source, 0xFFFFF4C2);

        assertEquals(0xFFFFD700, source.color().argb());
        assertNotEquals(source.color().argb(), readable.color().argb());
        assertTrue(ColorUtils.contrastRatio(readable.color().argb(), 0xFFFFF4C2) >= 4.5);
    }

    private static List<Integer> renderedSegmentColors(net.minecraft.util.text.ITextComponent component) {
        List<Integer> colors = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                assertNotNull(style.getColor());
                colors.add(style.getColor().getValue());
            }
            return Optional.empty();
        }, net.minecraft.util.text.Style.EMPTY);
        return colors;
    }
}
