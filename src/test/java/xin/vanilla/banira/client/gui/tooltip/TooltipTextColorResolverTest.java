package xin.vanilla.banira.client.gui.tooltip;

import net.minecraft.util.text.*;
import org.junit.Test;
import xin.vanilla.banira.client.enums.EnumTooltipTextColorPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class TooltipTextColorResolverTest {

    @Test
    public void itemPolicyMapsBlackWhiteAndUnstyledTextToPopupThemeColor() {
        IFormattableTextComponent source = new StringTextComponent("")
                .append(new StringTextComponent("plain"))
                .append(colored(" white", 0xFFFFFF))
                .append(colored(" black", 0x000000))
                .append(colored(" semantic", 0x7A1F1F));

        ITextComponent resolved = TooltipTextColorResolver.resolve(source, 0xFF245A36,
                new int[]{0xFFF0FFF0}, EnumTooltipTextColorPolicy.REPLACE_BLACK_AND_WHITE);

        assertEquals(Arrays.asList(0x245A36, 0x245A36, 0x245A36, 0x7A1F1F), colors(resolved));
    }

    @Test
    public void neutralReplacementCanAvoidOnlyWhiteOrOnlyBlack() {
        IFormattableTextComponent source = new StringTextComponent("")
                .append(colored("white", 0xFFFFFF))
                .append(colored("black", 0x000000));

        ITextComponent avoidWhite = TooltipTextColorResolver.resolve(source, 0xFF245A36,
                new int[]{0xFFF0FFF0}, EnumTooltipTextColorPolicy.REPLACE_WHITE);
        ITextComponent avoidBlack = TooltipTextColorResolver.resolve(source, 0xFFE2F4EF,
                new int[]{0xFF18212B}, EnumTooltipTextColorPolicy.REPLACE_BLACK);

        assertEquals(Arrays.asList(0x245A36, 0x000000), colors(avoidWhite));
        assertEquals(Arrays.asList(0xFFFFFF, 0xE2F4EF), colors(avoidBlack));
    }

    @Test
    public void lowContrastSemanticColorKeepsItsHueWhileBecomingReadable() {
        ITextComponent source = colored("yellow", 0xFFD700);

        ITextComponent resolved = TooltipTextColorResolver.resolve(source, 0xFF245A36,
                new int[]{0xFFFFF4C2}, EnumTooltipTextColorPolicy.PRESERVE);

        int color = colors(resolved).get(0);
        assertNotEquals(0xFFD700, color);
        assertNotEquals(0x000000, color);
        assertNotEquals(0xFFFFFF, color);
        assertTrue(((color >> 16) & 0xFF) >= ((color >> 8) & 0xFF));
        assertTrue(((color >> 8) & 0xFF) > (color & 0xFF));
    }

    @Test
    public void legacyFormattingUsesTheSameTextureBackgroundPalette() {
        ITextComponent source = new StringTextComponent("\u00A7eYellow");

        ITextComponent resolved = TooltipTextColorResolver.resolve(source, 0xFF245A36,
                new int[]{0xFFFFF4C2, 0xFFF4E7B1}, EnumTooltipTextColorPolicy.PRESERVE);

        assertEquals("Yellow", resolved.getString());
        int color = colors(resolved).get(0);
        assertNotEquals(0xFFFF55, color);
        assertNotEquals(0x000000, color);
        assertNotEquals(0xFFFFFF, color);
    }

    @Test
    public void adaptingColorPreservesTextDecorations() {
        Style decorated = Style.EMPTY.withColor(net.minecraft.util.text.Color.fromRgb(0xFFD700))
                .withBold(true).withItalic(true).withUnderlined(true)
                .applyFormat(TextFormatting.STRIKETHROUGH);

        ITextComponent resolved = TooltipTextColorResolver.resolve(
                new StringTextComponent("styled").setStyle(decorated), 0xFF245A36,
                new int[]{0xFFFFF4C2}, EnumTooltipTextColorPolicy.PRESERVE);
        Style rendered = firstVisibleStyle(resolved);

        assertTrue(rendered.isBold());
        assertTrue(rendered.isItalic());
        assertTrue(rendered.isUnderlined());
        assertTrue(rendered.isStrikethrough());
    }

    private static IFormattableTextComponent colored(String text, int rgb) {
        return new StringTextComponent(text).setStyle(
                Style.EMPTY.withColor(net.minecraft.util.text.Color.fromRgb(rgb)));
    }

    private static List<Integer> colors(ITextComponent component) {
        List<Integer> result = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                result.add(style.getColor() != null ? style.getColor().getValue() : null);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static Style firstVisibleStyle(ITextComponent component) {
        Style[] result = {Style.EMPTY};
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                result[0] = style;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result[0];
    }
}
