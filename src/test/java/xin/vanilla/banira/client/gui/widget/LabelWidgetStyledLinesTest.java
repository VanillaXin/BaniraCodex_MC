package xin.vanilla.banira.client.gui.widget;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.common.data.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LabelWidgetStyledLinesTest {

    @Test
    public void splittingLinesPreservesEachSegmentsColor() {
        IFormattableTextComponent source = new StringTextComponent("")
                .append(new StringTextComponent("Rare item").withStyle(TextFormatting.AQUA))
                .append(new StringTextComponent("\n"))
                .append(new StringTextComponent("Description").withStyle(TextFormatting.GRAY));

        List<ITextComponent> lines = LabelWidget.splitStyledLines(source);

        assertEquals(2, lines.size());
        assertEquals("Rare item", lines.get(0).getString());
        assertEquals("Description", lines.get(1).getString());
        assertNotNull(lines.get(0).getSiblings().get(0).getStyle().getColor());
        assertNotNull(lines.get(1).getSiblings().get(0).getStyle().getColor());
        assertEquals(0x55FFFF, lines.get(0).getSiblings().get(0).getStyle().getColor().getValue());
        assertEquals(0xAAAAAA, lines.get(1).getSiblings().get(0).getStyle().getColor().getValue());
    }

    @Test
    public void popupThemeColorDoesNotOverwriteRichTooltipStyles() {
        IFormattableTextComponent original = new StringTextComponent("Rare item")
                .withStyle(TextFormatting.AQUA);
        Text text = new Text(BaniraComponent.get().object(original));
        FontDrawArgs args = FontDrawArgs.ofPopo(text).preserveTextStyles(true);

        TooltipWidget.applyPopupTextColor(args, Color.argb(0xFFFFFFFF));

        ITextComponent rendered = args.text().toComponent().toVanilla();
        assertNotNull(rendered.getStyle().getColor());
        assertEquals(0x55FFFF, rendered.getStyle().getColor().getValue());
    }

    @Test
    public void popupThemeColorAppliesOnlyToUnstyledTooltipText() {
        IFormattableTextComponent original = new StringTextComponent("")
                .append(new StringTextComponent("Plain"))
                .append(new StringTextComponent(" Rare").withStyle(TextFormatting.AQUA));
        Text text = new Text(BaniraComponent.get().object(original));
        FontDrawArgs args = FontDrawArgs.ofPopo(text).preserveTextStyles(true);

        TooltipWidget.applyPopupTextColor(args, Color.argb(0xFF245A36));

        List<ITextComponent> wrapped = LabelWidget.preserveStyledOutputLines(
                args.text().toComponent().toVanilla(), Arrays.asList("Plain", "Rare"));
        assertEquals(Arrays.asList(0x245A36), visibleColors(wrapped.get(0)));
        assertEquals(Arrays.asList(0x55FFFF), visibleColors(wrapped.get(1)));
    }

    @Test
    public void truncatingAStyledLineKeepsItsColor() {
        ITextComponent original = new StringTextComponent("Rare item")
                .withStyle(TextFormatting.AQUA);

        ITextComponent rendered = LabelWidget.styledLine(
                original, "Rare item", "Rare...", "...",
                xin.vanilla.banira.client.enums.EnumEllipsisPosition.END);

        assertEquals("Rare...", rendered.getString());
        Integer[] firstVisibleColor = {null};
        rendered.visit((style, segment) -> {
            if (!segment.isEmpty() && firstVisibleColor[0] == null && style.getColor() != null) {
                firstVisibleColor[0] = style.getColor().getValue();
            }
            return Optional.empty();
        }, net.minecraft.util.text.Style.EMPTY);
        assertEquals(Integer.valueOf(0x55FFFF), firstVisibleColor[0]);
    }

    @Test
    public void wrappingOneTooltipLineKeepsColorsOnEveryOutputLine() {
        IFormattableTextComponent source = new StringTextComponent("")
                .append(new StringTextComponent("Rare item").withStyle(TextFormatting.AQUA))
                .append(new StringTextComponent("\n"))
                .append(new StringTextComponent("Long description").withStyle(TextFormatting.GRAY));

        List<ITextComponent> rendered = LabelWidget.preserveStyledOutputLines(
                source, Arrays.asList("Rare item", "Long", "description"));

        assertEquals(3, rendered.size());
        assertEquals(0x55FFFF, firstVisibleColor(rendered.get(0)));
        assertEquals(0xAAAAAA, firstVisibleColor(rendered.get(1)));
        assertEquals(0xAAAAAA, firstVisibleColor(rendered.get(2)));
    }

    @Test
    public void convertedItemTooltipKeepsRarityAndDescriptionColorsAfterWrapping() {
        xin.vanilla.banira.common.data.Component tooltip = BaniraComponent.get().empty();
        tooltip.append(BaniraComponent.get().object(
                new StringTextComponent("Rare item").withStyle(TextFormatting.AQUA)));
        tooltip.append("\n");
        tooltip.append(BaniraComponent.get().object(
                new StringTextComponent("Long description").withStyle(TextFormatting.GRAY)));

        ITextComponent source = new Text(tooltip).toComponent().toVanilla();
        List<ITextComponent> rendered = LabelWidget.preserveStyledOutputLines(
                source, Arrays.asList("Rare item", "Long", "description"));

        assertEquals(0x55FFFF, firstVisibleColor(rendered.get(0)));
        assertEquals(0xAAAAAA, firstVisibleColor(rendered.get(1)));
        assertEquals(0xAAAAAA, firstVisibleColor(rendered.get(2)));
    }

    private static int firstVisibleColor(ITextComponent component) {
        Integer[] color = {null};
        component.visit((style, segment) -> {
            if (!segment.isEmpty() && color[0] == null && style.getColor() != null) {
                color[0] = style.getColor().getValue();
            }
            return Optional.empty();
        }, net.minecraft.util.text.Style.EMPTY);
        assertNotNull(color[0]);
        return color[0];
    }

    private static List<Integer> visibleColors(ITextComponent component) {
        List<Integer> colors = new ArrayList<>();
        component.visit((style, segment) -> {
            if (!segment.isEmpty()) {
                colors.add(style.getColor() == null ? null : style.getColor().getValue());
            }
            return Optional.empty();
        }, net.minecraft.util.text.Style.EMPTY);
        return colors;
    }
}
