package xin.vanilla.banira.client.gui;

import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;
import java.lang.reflect.Field;
import java.util.ArrayList;
import static org.junit.Assert.*;

public class NotificationWheelRoutingTest {
    @Before public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }

    @Test public void focusedDetailBarCannotStealListWheel() throws Exception {
        Fixture f = new Fixture(true);
        assertTrue(f.screen.mouseScrolled(20, 40, -1));
        assertEquals(1, f.left.value(), 0);
        assertEquals(20, f.right.value(), 0);
    }
    @Test public void focusedPaneDoesNotConsumeOutsideBothAreas() throws Exception {
        Fixture f = new Fixture(true);
        assertFalse(f.screen.mouseScrolled(300, 200, -1));
        assertEquals(0, f.left.value(), 0);
        assertEquals(20, f.right.value(), 0);
    }
    @Test public void ordinaryScrollbarsKeepTheirFocusedWheelBehavior() throws Exception {
        Fixture f = new Fixture(false);
        assertTrue(f.screen.mouseScrolled(300, 200, -1));
        assertEquals(21, f.right.value(), 0);
    }

    private static final class Fixture {
        final NotificationLogScreen screen;
        final ScrollbarWidget left, right;
        Fixture(boolean paneOnly) throws Exception {
            Class<?> type = Class.forName("sun.misc.Unsafe");
            Field field = type.getDeclaredField("theUnsafe"); field.setAccessible(true);
            screen = (NotificationLogScreen) type.getMethod("allocateInstance", Class.class)
                    .invoke(field.get(null), NotificationLogScreen.class);
            set("widgets", new ArrayList<>());
            set("inputState", xin.vanilla.banira.client.util.InputStateManager.instance());
            Field children = net.minecraft.client.gui.screens.Screen.class.getDeclaredField("children");
            children.setAccessible(true); children.set(screen, new ArrayList<>());
            set("cursor", new MouseWidget(null, false));
            set("popupOption", new PopupOption(screen));
            Field lines = NotificationLogScreen.class.getDeclaredField("detailContentLines");
            lines.setAccessible(true); lines.set(screen, java.util.Collections.emptyList());
            left = new ScrollbarWidget(null, new ScreenCoordinate(94, 10, 6, 100));
            left.addScrollHoverArea(new ScreenCoordinate(10, 10, 80, 100));
            right = new ScrollbarWidget(null, new ScreenCoordinate(204, 10, 6, 100));
            left.maxValue(100).scrollStep(1); right.maxValue(100).scrollStep(1).value(20);
            right.focused(true);
            if (paneOnly) {
                try {
                    ScrollbarWidget.class.getMethod("scrollOnFocus", boolean.class).invoke(left, false);
                    ScrollbarWidget.class.getMethod("scrollOnFocus", boolean.class).invoke(right, false);
                } catch (NoSuchMethodException original) { }
            }
            screen.widgets().add(left); screen.widgets().add(right);
        }
        private void set(String name, Object value) throws Exception {
            Field f = BaniraScreen.class.getDeclaredField(name); f.setAccessible(true); f.set(screen, value);
        }
    }
}
