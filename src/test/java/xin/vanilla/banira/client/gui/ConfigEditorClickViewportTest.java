package xin.vanilla.banira.client.gui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.ScrollbarWidget;
import xin.vanilla.banira.internal.client.ConfigEditorViewportModel;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class ConfigEditorClickViewportTest {
    @Before
    public void setUp() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @After
    public void tearDown() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void scrolledListCannotInterceptSearchOrFooterClicks() throws Exception {
        ConfigEditorScreen screen = allocate(ConfigEditorScreen.class);
        ConfigEditorViewportModel viewport = new ConfigEditorViewportModel(10, 10, 6, 2, 22);
        viewport.resize(320, 240);
        viewport.layoutContent(500, 150, new ScrollbarWidget(null));
        viewport.applyScrollbarValue(20);
        CollapsiblePanelWidget root = new CollapsiblePanelWidget(null);
        viewport.applyContentBounds(root);
        set(screen, "viewport", viewport);
        set(screen, "contentRootPanel", root);

        assertTrue("scrolled header overlaps search bounds", root.isMouseInside(25, 25));
        assertFalse("search must receive the click", screen.shouldWidgetReceiveClick(root, MouseEvent.of(25, 25, 0)));
        assertFalse("footer must receive the click", screen.shouldWidgetReceiveClick(root, MouseEvent.of(25, 200, 0)));
    }

    @Test
    public void visibleRowsRemainClickableAndOverlaysAreNotClipped() throws Exception {
        ConfigEditorScreen screen = allocate(ConfigEditorScreen.class);
        ConfigEditorViewportModel viewport = new ConfigEditorViewportModel(10, 10, 6, 2, 22);
        viewport.resize(320, 240);
        viewport.layoutContent(500, 150, new ScrollbarWidget(null));
        CollapsiblePanelWidget root = new CollapsiblePanelWidget(null);
        viewport.applyContentBounds(root);
        set(screen, "viewport", viewport);
        set(screen, "contentRootPanel", root);
        assertTrue(screen.shouldWidgetReceiveClick(root, MouseEvent.of(20, 42, 0)));
        assertTrue(screen.shouldWidgetReceiveClick(root, MouseEvent.of(25, 191, 0)));
        assertFalse(screen.shouldWidgetReceiveClick(root, MouseEvent.of(25, 192, 0)));
        assertFalse(screen.shouldWidgetReceiveClick(root, MouseEvent.of(19, 60, 0)));
        assertTrue(screen.shouldWidgetReceiveClick(new CollapsiblePanelWidget(null,
                new ScreenCoordinate(20, 20, 100, 100)), MouseEvent.of(25, 25, 0)));
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = ConfigEditorScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }
}
