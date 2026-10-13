package xin.vanilla.banira.client.minimap;

import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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
    private List<Component> wrap(Component text, int width) throws Exception {
        StringSplitter splitter = new StringSplitter((point, style) -> style.isBold() ? 7 : 6);
        Method wrap = layout().getMethod("wrap", Component.class, StringSplitter.class, int.class);
        return (List<Component>) wrap.invoke(null, text, splitter, width);
    }

    @Test
    public void longNativeTextWrapsWithoutLosingColorAndBold() throws Exception {
        Component text = net.minecraft.network.chat.Component.literal("abcdefghijkl").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        List<Component> lines = wrap(text, 21);
        assertEquals(4, lines.size());
        assertEquals("abcdefghijkl", lines.stream().map(Component::getString).reduce("", String::concat));
        StringSplitter splitter = new StringSplitter((point, style) -> style.isBold() ? 7 : 6);
        for (Component line : lines) {
            assertTrue(splitter.stringWidth(line) <= 21);
            line.visit((style, content) -> {
                if (content.isEmpty()) return java.util.Optional.empty();
                assertTrue(style.isBold());
                assertEquals(ChatFormatting.RED.getColor().intValue(), style.getColor().getValue());
                return java.util.Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
        }
    }

    @Test
    public void explicitNewlinesKeepSeparateNativeComponents() throws Exception {
        List<Component> lines = wrap(net.minecraft.network.chat.Component.literal("aaaa\nbbbb"), 30);
        assertEquals(2, lines.size());
        assertEquals("aaaa", lines.get(0).getString());
        assertEquals("bbbb", lines.get(1).getString());
    }
}
