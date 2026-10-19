package xin.vanilla.banira.client.gui;

import net.minecraft.network.chat.Component;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.MouseWidget;
import xin.vanilla.banira.client.gui.widget.PopupOption;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.*;

public class PopupMouseRoutingTest {
    @Before public void platform() { BaniraPlatforms.install(new TestBaniraPlatform()); }

    @Test public void pressUsesEventCoordinatesBeforeHoverHasRendered() throws Exception {
        Fixture f = new Fixture();
        assertFalse(f.popup.isHovered());
        assertTrue(f.screen.mouseClicked(20, 20, 0));
        assertTrue(f.screen.mouseReleased(20, 20, 0));
        assertEquals(1, f.popup.selected);
        assertEquals(0, f.screen.backgroundPresses);
    }

    @Test public void releaseReachesThePressedOptionWithoutCachedHover() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.popup.tryHandleOptionPress(MouseEvent.of(20, 20, 0)));
        assertTrue(f.screen.mouseReleased(20, 20, 0));
        assertEquals(1, f.popup.selected);
        assertEquals(0, f.screen.backgroundReleases);
        f.screen.mouseReleased(20, 20, 0);
        assertEquals(1, f.popup.selected);
    }

    @Test public void draggingOutConsumesReleaseWithoutSelectingOrReleasingUnderlyingControls() throws Exception {
        Fixture f = new Fixture();
        assertTrue(f.popup.tryHandleOptionPress(MouseEvent.of(20, 20, 0)));
        assertTrue(f.screen.mouseReleased(200, 200, 0));
        assertEquals(0, f.popup.selected);
        assertEquals(0, f.screen.backgroundReleases);
    }

    @Test public void anUnownedReleaseStillReachesTheScreen() throws Exception {
        Fixture f = new Fixture();
        f.screen.mouseReleased(200, 200, 0);
        assertEquals(1, f.screen.backgroundReleases);
        assertEquals(0, f.popup.selected);
    }

    private static final class TestScreen extends BaniraScreen {
        int backgroundPresses, backgroundReleases;
        private TestScreen() { super(Component.empty()); }
        @Override protected void onRender(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) { }
        @Override protected void onMouseClicked(MouseClickedHandleArgs args) { backgroundPresses++; }
        @Override protected void onMouseReleased(MouseReleasedHandleArgs args) { backgroundReleases++; }
    }

    private static final class EventPopup extends PopupOption {
        boolean pressed;
        int selected;
        EventPopup(BaniraScreen screen) { super(screen); }
        @Override public boolean isHovered() { return false; }
        private boolean inside(MouseEvent event) { return event.mouseX() >= 10 && event.mouseX() < 60 && event.mouseY() >= 10 && event.mouseY() < 60; }
        @Override public boolean tryHandleOptionPress(MouseEvent event) { pressed = inside(event); return pressed; }
        @Override public boolean tryHandleOptionRelease(MouseEvent event) {
            if (!pressed) return false;
            pressed = false;
            if (inside(event)) selected++;
            return true;
        }
        @Override public PopupOption clear() { pressed = false; return this; }
    }

    private static final class Fixture {
        final TestScreen screen;
        final EventPopup popup;
        Fixture() throws Exception {
            Class<?> type = Class.forName("sun.misc.Unsafe");
            Field unsafe = type.getDeclaredField("theUnsafe"); unsafe.setAccessible(true);
            screen = (TestScreen) type.getMethod("allocateInstance", Class.class).invoke(unsafe.get(null), TestScreen.class);
            set(BaniraScreen.class, "widgets", new ArrayList<>());
            set(BaniraScreen.class, "focusableWidgets", new HashSet<>());
            set(BaniraScreen.class, "cursor", new MouseWidget(null, false));
            set(net.minecraft.client.gui.screens.Screen.class, "children", new ArrayList<>());
            popup = new EventPopup(screen);
            set(BaniraScreen.class, "popupOption", popup);
        }
        private void set(Class<?> type, String name, Object value) throws Exception {
            Field field = type.getDeclaredField(name); field.setAccessible(true); field.set(screen, value);
        }
    }
}
