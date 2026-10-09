package xin.vanilla.banira.client.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.CharacterManager;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NotificationDetailScreenTest {
    @Test public void visibleContentKeepsItsStyle() throws Exception {
        assertEquals("https://example.com/0", style(screen(), 117, 61).getClickEvent().getValue());
    }

    @Test public void bottomEdgeDoesNotExposeClippedContent() throws Exception {
        assertNull(style(screen(), 117, 114));
    }

    @Test public void belowPanelDoesNotExposeClippedContent() throws Exception {
        assertNull(style(screen(), 117, 150));
    }

    @Test public void horizontalEdgesDoNotExposeStyles() throws Exception {
        NotificationLogScreen screen = screen();
        assertNull(style(screen, 115, 61));
        assertNull(style(screen, 204, 61));
    }

    @Test public void scrollingChangesTheStyledSourceLine() throws Exception {
        NotificationLogScreen screen = screen();
        BaniraScreen.MouseScrolledHandleArgs args = new BaniraScreen.MouseScrolledHandleArgs()
                .mouseX(117).mouseY(61).delta(-3);
        screen.onMouseScrolled(args);
        assertTrue(args.consumed());
        assertEquals("https://example.com/3", style(screen, 117, 61).getClickEvent().getValue());
    }

    @Test public void scrollingOutsideContentLeavesItUnchanged() throws Exception {
        NotificationLogScreen screen = screen();
        BaniraScreen.MouseScrolledHandleArgs args = new BaniraScreen.MouseScrolledHandleArgs()
                .mouseX(115).mouseY(61).delta(-3);
        screen.onMouseScrolled(args);
        assertFalse(args.consumed());
        assertEquals("https://example.com/0", style(screen, 117, 61).getClickEvent().getValue());
    }

    private static NotificationLogScreen screen() throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field instance = unsafe.getDeclaredField("theUnsafe"); instance.setAccessible(true);
        NotificationLogScreen screen = (NotificationLogScreen) unsafe.getMethod("allocateInstance", Class.class)
                .invoke(instance.get(null), NotificationLogScreen.class);
        FontRenderer font = new FontRenderer(id -> null);
        set(FontRenderer.class, font, "splitter", new CharacterManager((codepoint, style) -> 6));
        set(Screen.class, screen, "font", font);
        set(BaniraScreen.class, screen, "widgets", new ArrayList<>());
        set(NotificationLogScreen.class, screen, "rightX", 100);
        set(NotificationLogScreen.class, screen, "rightY", 10);
        set(NotificationLogScreen.class, screen, "rightW", 120);
        set(NotificationLogScreen.class, screen, "rightH", 120);
        set(NotificationLogScreen.class, screen, "detailContentLeft", 116.0);
        set(NotificationLogScreen.class, screen, "detailContentTop", 60.0);
        set(NotificationLogScreen.class, screen, "detailContentMaxLineW", 88);
        List<IReorderingProcessor> lines = new ArrayList<>();
        for (int i = 0; i < 20; i++) lines.add(IReorderingProcessor.forward("link", Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.com/" + i))));
        set(NotificationLogScreen.class, screen, "detailContentLines", lines);
        try {
            Field field = NotificationLogScreen.class.getDeclaredField("detailViewport");
            field.setAccessible(true);
            Object viewport = field.getType().getConstructor().newInstance();
            field.set(screen, viewport);
            field.getType().getMethod("layout", int.class, int.class, int.class, int.class, int.class, int.class)
                    .invoke(viewport, 116, 60, 88, 54, 9, 20);
        } catch (NoSuchFieldException oldScreen) {
            // The original screen has no detail viewport.
        }
        return screen;
    }

    private static Style style(NotificationLogScreen screen, double x, double y) throws Exception {
        Method method = NotificationLogScreen.class.getDeclaredMethod("styleAtDetailContentPoint", double.class, double.class);
        method.setAccessible(true);
        return (Style) method.invoke(screen, x, y);
    }

    private static void set(Class<?> type, Object target, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
}
