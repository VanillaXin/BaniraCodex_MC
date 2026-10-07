package xin.vanilla.banira.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class PlayerConfigTooltipTest {
    private ProbeScreen screen;
    private PoseStack graphics;

    @Before
    public void setup() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        BaniraPlatforms.install(new TestBaniraPlatform().development(true));
        screen = new ProbeScreen();
        graphics = new PoseStack();
    }

    @After
    public void cleanup() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void missingDescriptionStillProvidesTheCompleteLabelTooltip() {
        LabelWidget label = screen.row("A very long player configuration explanation", null, false);
        TooltipWidget tooltip = childTooltip(label);
        assertNotNull("Full labels must not be lost when clipped", tooltip);
        assertEquals("A very long player configuration explanation", tooltip.text().content());
    }

    @Test
    public void explicitDescriptionTooltipIncludesBothFullTitleAndDescription() {
        LabelWidget label = screen.row("Complete title", "Additional details", true);
        TooltipWidget tooltip = screen.explicit;
        assertEquals("Complete title\nAdditional details", tooltip.text().content());
        assertNull("An explicit tooltip must not be duplicated", childTooltip(label));
    }

    @Test
    public void scrolledPanelCannotStealSearchOrFooterClicksButVisibleRowsCan() {
        for (int i = 0; i < 12; i++) screen.row("Explanation " + i, null, false);
        assertTrue("The scrolled panel geometrically covers the search", screen.panel.isMouseInside(25, 25));
        assertFalse(screen.shouldWidgetReceiveClick(screen.panel, MouseEvent.of(25, 25, 0)));
        assertFalse(screen.shouldWidgetReceiveClick(screen.panel, MouseEvent.of(25, 150, 0)));
        assertTrue(screen.shouldWidgetReceiveClick(screen.panel, MouseEvent.of(25, 45, 0)));
        assertFalse(screen.shouldWidgetReceiveClick(screen.panel, MouseEvent.of(180, 50, 0)));
        assertFalse(screen.shouldWidgetReceiveClick(screen.panel, MouseEvent.of(25, 140, 0)));
        assertTrue("Search and popup widgets keep their own input route",
                screen.shouldWidgetReceiveClick(new LabelWidget(screen), MouseEvent.of(25, 25, 0)));
    }

    @Test
    public void labelsScrolledUnderTheSearchBarDoNotShowTheirTooltip() throws Exception {
        screen.row("Explanation", "Details", true);
        at(25, 25, () -> screen.explicit.render(graphics, 0));
        assertEquals("The list viewport starts below the search bar", 0, screen.requests);
        screen.panel.bounds(new ScreenCoordinate(20, 50, 160, 80));
        at(25, 55, () -> screen.explicit.render(graphics, 0));
        assertEquals(1, screen.requests);
        at(25, 170, () -> screen.explicit.render(graphics, 0));
        assertEquals("Leaving the viewport ends the tooltip", 1, screen.requests);
    }

    @Test
    public void dropdownsSuppressConfigurationTooltips() throws Exception {
        screen.row("Explanation", "Details", true);
        screen.panel.bounds(new ScreenCoordinate(20, 50, 160, 80));
        screen.dropdown = true;
        at(25, 55, () -> screen.explicit.render(graphics, 0));
        assertEquals(0, screen.requests);
    }

    private void at(double x, double y, Runnable action) throws Exception {
        Object input = screen.inputState();
        Field fx = input.getClass().getDeclaredField("mouseX"), fy = input.getClass().getDeclaredField("mouseY");
        fx.setAccessible(true);
        fy.setAccessible(true);
        double oldX = fx.getDouble(input), oldY = fy.getDouble(input);
        try { fx.setDouble(input, x); fy.setDouble(input, y); action.run(); }
        finally { fx.setDouble(input, oldX); fy.setDouble(input, oldY); }
    }

    private static TooltipWidget childTooltip(IWidget widget) {
        for (IWidget child : widget.children()) if (child instanceof TooltipWidget) return (TooltipWidget) child;
        return null;
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static final class ProbeScreen extends PlayerConfigScreen {
        final CollapsiblePanelWidget panel;
        TooltipWidget explicit;
        int requests;
        boolean dropdown;

        ProbeScreen() throws Exception {
            super("probe", BaniraComponent.get().literal("Player settings"), null, null, null);
            panel = CollapsiblePanelWidget.createAutoHeight(this, 20, 20, 160);
            panel.headerHeight(0);
            set("rootPanel", panel);
            set("contentLeft", 20);
            set("contentWidth", 160);
            set("listTop", 40);
            set("listAreaHeight", 100);
        }

        private void set(String name, Object value) throws Exception {
            Field field = PlayerConfigScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(this, value);
        }

        LabelWidget row(String title, String description, boolean hasTooltip) {
            BaseWidget row = new BaseWidget(this) {
                @Override public void render(PoseStack graphics, float partialTicks) {}
            };
            row.bounds(new ScreenCoordinate(0, 0, 160, 20));
            LabelWidget label = new LabelWidget(this);
            label.bounds(new ScreenCoordinate(0, 0, 64, 20));
            label.text(Text.literal(title));
            label.textWrap(false);
            row.addChild(label);
            Component detail = description == null ? null : BaniraComponent.get().literal(description);
            if (hasTooltip) {
                explicit = new TooltipWidget(this, new ScreenCoordinate(0, 0, 64, 20));
                explicit.text(detail);
                explicit.popupAtScreenCoords(true);
                row.addChild(explicit);
            }
            addPlayerRow(panel, row, 20, label, hasTooltip ? explicit : null,
                    BaniraComponent.get().literal(title), detail);
            return label;
        }

        @Override protected void buildPlayerConfig(CollapsiblePanelWidget root) {}
        @Override protected void savePlayerConfig() {}
        @Override public boolean isAnyDropdownSelectOpen() { return dropdown; }
        @Override public void addDeferredTooltipRender(Consumer<PoseStack> render) { requests++; }
    }
}
