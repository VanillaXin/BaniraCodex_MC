package xin.vanilla.banira.client.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        MutableComponent source = Component.empty()
                .append(Component.literal("Rare item").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("\n"))
                .append(Component.literal("Description").withStyle(ChatFormatting.GRAY));

        List<Component> lines = LabelWidget.splitStyledLines(source);

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
        MutableComponent original = Component.literal("Rare item")
                .withStyle(ChatFormatting.AQUA);
        Text text = new Text(BaniraComponent.get().object(original));
        FontDrawArgs args = FontDrawArgs.ofPopo(text).preserveTextStyles(true);

        TooltipWidget.applyPopupTextColor(args, Color.argb(0xFFFFFFFF));

        Component rendered = args.text().toComponent().toVanilla();
        assertNotNull(rendered.getStyle().getColor());
        assertEquals(0x55FFFF, rendered.getStyle().getColor().getValue());
    }

    @Test
    public void popupThemeColorAppliesOnlyToUnstyledTooltipText() {
        MutableComponent original = Component.empty()
                .append(Component.literal("Plain"))
                .append(Component.literal(" Rare").withStyle(ChatFormatting.AQUA));
        Text text = new Text(BaniraComponent.get().object(original));
        FontDrawArgs args = FontDrawArgs.ofPopo(text).preserveTextStyles(true);

        TooltipWidget.applyPopupTextColor(args, Color.argb(0xFF245A36));

        List<Component> wrapped = LabelWidget.preserveStyledOutputLines(
                args.text().toComponent().toVanilla(), Arrays.asList("Plain", "Rare"));
        assertEquals(Arrays.asList(0x245A36), visibleColors(wrapped.get(0)));
        assertEquals(Arrays.asList(0x55FFFF), visibleColors(wrapped.get(1)));
    }

    @Test
    public void truncatingAStyledLineKeepsItsColor() {
        Component original = Component.literal("Rare item")
                .withStyle(ChatFormatting.AQUA);

        Component rendered = LabelWidget.styledLine(
                original, "Rare item", "Rare...", "...",
                xin.vanilla.banira.client.enums.EnumEllipsisPosition.END);

        assertEquals("Rare...", rendered.getString());
        Integer[] firstVisibleColor = {null};
        rendered.visit((style, segment) -> {
            if (!segment.isEmpty() && firstVisibleColor[0] == null && style.getColor() != null) {
                firstVisibleColor[0] = style.getColor().getValue();
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        assertEquals(Integer.valueOf(0x55FFFF), firstVisibleColor[0]);
    }

    @Test
    public void wrappingOneTooltipLineKeepsColorsOnEveryOutputLine() {
        MutableComponent source = Component.empty()
                .append(Component.literal("Rare item").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("\n"))
                .append(Component.literal("Long description").withStyle(ChatFormatting.GRAY));

        List<Component> rendered = LabelWidget.preserveStyledOutputLines(
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
                Component.literal("Rare item").withStyle(ChatFormatting.AQUA)));
        tooltip.append("\n");
        tooltip.append(BaniraComponent.get().object(
                Component.literal("Long description").withStyle(ChatFormatting.GRAY)));

        Component source = new Text(tooltip).toComponent().toVanilla();
        List<Component> rendered = LabelWidget.preserveStyledOutputLines(
                source, Arrays.asList("Rare item", "Long", "description"));

        assertEquals(0x55FFFF, firstVisibleColor(rendered.get(0)));
        assertEquals(0xAAAAAA, firstVisibleColor(rendered.get(1)));
        assertEquals(0xAAAAAA, firstVisibleColor(rendered.get(2)));
    }

    private static int firstVisibleColor(Component component) {
        Integer[] color = {null};
        component.visit((style, segment) -> {
            if (!segment.isEmpty() && color[0] == null && style.getColor() != null) {
                color[0] = style.getColor().getValue();
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        assertNotNull(color[0]);
        return color[0];
    }

    private static List<Integer> visibleColors(Component component) {
        List<Integer> colors = new ArrayList<>();
        component.visit((style, segment) -> {
            if (!segment.isEmpty()) {
                colors.add(style.getColor() == null ? null : style.getColor().getValue());
            }
            return Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return colors;
    }
}
