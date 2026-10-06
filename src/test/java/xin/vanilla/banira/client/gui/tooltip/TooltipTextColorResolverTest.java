package xin.vanilla.banira.client.gui.tooltip;

import net.minecraft.network.chat.*;
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
        MutableComponent source = new TextComponent("")
                .append(new TextComponent("plain"))
                .append(colored(" white", 0xFFFFFF))
                .append(colored(" black", 0x000000))
                .append(colored(" semantic", 0x7A1F1F));

        Component resolved = TooltipTextColorResolver.resolve(source, 0xFF245A36,
                new int[]{0xFFF0FFF0}, EnumTooltipTextColorPolicy.REPLACE_BLACK_AND_WHITE);

        assertEquals(Arrays.asList(0x245A36, 0x245A36, 0x245A36, 0x7A1F1F), colors(resolved));
    }

    @Test
    public void neutralReplacementCanAvoidOnlyWhiteOrOnlyBlack() {
        MutableComponent source = new TextComponent("")
                .append(colored("white", 0xFFFFFF))
                .append(colored("black", 0x000000));

        Component avoidWhite = TooltipTextColorResolver.resolve(source, 0xFF245A36,
                new int[]{0xFFF0FFF0}, EnumTooltipTextColorPolicy.REPLACE_WHITE);
        Component avoidBlack = TooltipTextColorResolver.resolve(source, 0xFFE2F4EF,
                new int[]{0xFF18212B}, EnumTooltipTextColorPolicy.REPLACE_BLACK);

        assertEquals(Arrays.asList(0x245A36, 0x000000), colors(avoidWhite));
        assertEquals(Arrays.asList(0xFFFFFF, 0xE2F4EF), colors(avoidBlack));
    }

    @Test
    public void lowContrastSemanticColorKeepsItsHueWhileBecomingReadable() {
        Component source = colored("yellow", 0xFFD700);

        Component resolved = TooltipTextColorResolver.resolve(source, 0xFF245A36,
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
        Component source = new TextComponent("\u00A7eYellow");

        Component resolved = TooltipTextColorResolver.resolve(source, 0xFF245A36,
                new int[]{0xFFFFF4C2, 0xFFF4E7B1}, EnumTooltipTextColorPolicy.PRESERVE);

        assertEquals("Yellow", resolved.getString());
        int color = colors(resolved).get(0);
        assertNotEquals(0xFFFF55, color);
        assertNotEquals(0x000000, color);
        assertNotEquals(0xFFFFFF, color);
    }

    @Test
    public void adaptingColorPreservesTextDecorations() {
        Style decorated = Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700))
                .withBold(true).withItalic(true).withUnderlined(true).withStrikethrough(true);

        Component resolved = TooltipTextColorResolver.resolve(
                new TextComponent("styled").setStyle(decorated), 0xFF245A36,
                new int[]{0xFFFFF4C2}, EnumTooltipTextColorPolicy.PRESERVE);
        Style rendered = firstVisibleStyle(resolved);

        assertTrue(rendered.isBold());
        assertTrue(rendered.isItalic());
        assertTrue(rendered.isUnderlined());
        assertTrue(rendered.isStrikethrough());
    }

    private static MutableComponent colored(String text, int rgb) {
        return new TextComponent(text).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    private static List<Integer> colors(Component component) {
        List<Integer> result = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                result.add(style.getColor() != null ? style.getColor().getValue() : null);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static Style firstVisibleStyle(Component component) {
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
