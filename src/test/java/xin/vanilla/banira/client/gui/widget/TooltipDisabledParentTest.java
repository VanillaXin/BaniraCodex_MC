package xin.vanilla.banira.client.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.lang.reflect.Field;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class TooltipDisabledParentTest {
    private ProbeScreen screen;
    private GuiGraphics graphics;
    private ButtonWidget button;
    private TooltipWidget tooltip;

    @Before
    public void setUp() throws Exception {
        BaniraPlatforms.install(new TestBaniraPlatform().development(true));
        screen = new ProbeScreen();
        graphics = allocate(GuiGraphics.class);
        for (Field field : GuiGraphics.class.getDeclaredFields()) {
            if (field.getType() == PoseStack.class) {
                field.setAccessible(true);
                field.set(graphics, new PoseStack());
            }
        }
        button = new ButtonWidget(screen);
        button.bounds(new ScreenCoordinate(20, 20, 20, 20));
        tooltip = new TooltipWidget(screen);
        tooltip.bounds(new ScreenCoordinate(0, 0, 20, 20));
        tooltip.popupAtScreenCoords(true);
        button.addChild(tooltip);
    }

    @After
    public void tearDown() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void disablingHoveredButtonDoesNotFreezeTooltipAfterMouseMoves() throws Exception {
        at(25, 25, button::update);
        assertTrue(tooltip.mouseInside);
        button.enabled(false);
        at(100, 100, () -> {
            button.update();
            tooltip.render(graphics, 0);
        });
        assertFalse("disabled parent must not retain an old hover", tooltip.mouseInside);
        assertEquals(0, screen.requests);
    }

    @Test
    public void disabledButtonTooltipUsesCurrentHoverAndEndsOnLeaving() throws Exception {
        button.enabled(false);
        at(25, 25, () -> tooltip.render(graphics, 0));
        assertEquals(1, screen.requests);
        at(40, 40, () -> tooltip.render(graphics, 0));
        assertFalse(tooltip.mouseInside);
        assertEquals(1, screen.requests);
    }

    private void at(double x, double y, Runnable action) throws Exception {
        Object input = screen.inputState();
        Field mouseX = input.getClass().getDeclaredField("mouseX");
        Field mouseY = input.getClass().getDeclaredField("mouseY");
        mouseX.setAccessible(true);
        mouseY.setAccessible(true);
        double oldX = mouseX.getDouble(input);
        double oldY = mouseY.getDouble(input);
        try {
            mouseX.setDouble(input, x);
            mouseY.setDouble(input, y);
            action.run();
        } finally {
            mouseX.setDouble(input, oldX);
            mouseY.setDouble(input, oldY);
        }
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type));
    }

    private static class ProbeScreen extends BaniraScreen {
        int requests;

        ProbeScreen() {
            super(net.minecraft.network.chat.Component.literal("tooltip probe"));
        }

        @Override
        public boolean isAnyDropdownSelectOpen() {
            return false;
        }

        @Override
        public void addDeferredTooltipRender(Consumer<GuiGraphics> render) {
            requests++;
        }

        @Override
        protected void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        }
    }
}
