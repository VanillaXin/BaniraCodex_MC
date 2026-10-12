package xin.vanilla.banira.client.minimap;

import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

public class MinimapTextLayoutTest {
    private Class<?> layout() throws Exception {
        String name = "xin.vanilla.banira.internal.client.minimap.MinimapTextLayout";
        assertNotNull("Missing native text layout", getClass().getClassLoader()
                .getResource(name.replace('.', '/') + ".class"));
        return Class.forName(name);
    }

    private double y(double after, double mapHeight, double textHeight, int screenHeight) throws Exception {
        return (double) layout().getMethod("nativeY", double.class, double.class, double.class, int.class)
                .invoke(null, after, mapHeight, textHeight, screenHeight);
    }

    @Test
    public void bottomInformationMovesAboveMapWithoutOverlap() throws Exception {
        assertEquals(298, y(463, 128, 31, 480), .001);
    }

    @Test
    public void topInformationKeepsNativeAnchor() throws Exception {
        assertEquals(136, y(136, 128, 31, 480), .001);
    }

    @Test
    public void insufficientVerticalRoomRemainsInsideViewport() throws Exception {
        assertEquals(0, y(43, 32, 70, 80), .001);
    }

    @SuppressWarnings("unchecked")
    private List<ITextComponent> wrap(ITextComponent text, int width) throws Exception {
        CharacterManager splitter = new CharacterManager((point, style) -> style.isBold() ? 7 : 6);
        Method wrap = layout().getMethod("wrap", ITextComponent.class, CharacterManager.class, int.class);
        return (List<ITextComponent>) wrap.invoke(null, text, splitter, width);
    }

    @Test
    public void longNativeTextWrapsWithoutLosingColorAndBold() throws Exception {
        ITextComponent text = new StringTextComponent("abcdefghijkl").withStyle(TextFormatting.RED, TextFormatting.BOLD);
        List<ITextComponent> lines = wrap(text, 21);
        assertEquals(4, lines.size());
        assertEquals("abcdefghijkl", lines.stream().map(ITextComponent::getString).reduce("", String::concat));
        CharacterManager splitter = new CharacterManager((point, style) -> style.isBold() ? 7 : 6);
        for (ITextComponent line : lines) {
            assertTrue(splitter.stringWidth(line) <= 21);
            line.visit((style, content) -> {
                if (content.isEmpty()) return java.util.Optional.empty();
                assertTrue(style.isBold());
                assertEquals(TextFormatting.RED.getColor().intValue(), style.getColor().getValue());
                return java.util.Optional.empty();
            }, net.minecraft.util.text.Style.EMPTY);
        }
    }

    @Test
    public void explicitNewlinesKeepSeparateNativeComponents() throws Exception {
        List<ITextComponent> lines = wrap(new StringTextComponent("aaaa\nbbbb"), 30);
        assertEquals(2, lines.size());
        assertEquals("aaaa", lines.get(0).getString());
        assertEquals("bbbb", lines.get(1).getString());
    }
}
