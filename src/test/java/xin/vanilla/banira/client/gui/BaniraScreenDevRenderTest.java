package xin.vanilla.banira.client.gui;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraInputState;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import static org.junit.Assert.*;

public class BaniraScreenDevRenderTest {
    private BaniraScreen screen;

    @Before
    public void setUp() {
        BaniraPlatforms.install(new TestBaniraPlatform().development(true));
        screen = new BaniraScreen(net.minecraft.network.chat.Component.literal("dev render")) {
            @Override
            protected void onRender(com.mojang.blaze3d.vertex.PoseStack stack, float partialTicks) { }
        };
    }

    @After
    public void tearDown() {
        BaniraPlatforms.install(new TestBaniraPlatform());
    }

    @Test
    public void callbackSeesCoordinatesAndSuccessRestoresSnapshot() {
        BaniraInputState input = screen.inputState();
        double x = input.mouseX(), y = input.mouseY();
        int[] calls = {0};
        screen.runDevRenderAt(12.5, 34.25, () -> {
            calls[0]++;
            assertSame(input, screen.inputState());
            assertPosition(12.5, 34.25);
        });
        assertEquals(1, calls[0]);
        assertPosition(x, y);
    }

    @Test
    public void throwingCallbackRestoresSnapshotAndPropagatesSameFailure() {
        double x = screen.inputState().mouseX(), y = screen.inputState().mouseY();
        RuntimeException failure = new IllegalStateException("render failed");
        assertSame(failure, assertThrows(IllegalStateException.class, () ->
                screen.runDevRenderAt(12, 34, () -> {
                    assertPosition(12, 34);
                    throw failure;
                })));
        assertPosition(x, y);
    }

    @Test
    public void nestedFailureRestoresOuterCoordinatesThenOriginalSnapshot() {
        double x = screen.inputState().mouseX(), y = screen.inputState().mouseY();
        AssertionError failure = new AssertionError("inner render failed");
        screen.runDevRenderAt(10, 20, () -> {
            assertPosition(10, 20);
            screen.runDevRenderAt(30, 40, () -> assertPosition(30, 40));
            assertPosition(10, 20);
            assertSame(failure, assertThrows(AssertionError.class, () ->
                    screen.runDevRenderAt(50, 60, () -> {
                        assertPosition(50, 60);
                        throw failure;
                    })));
            assertPosition(10, 20);
        });
        assertPosition(x, y);
    }

    @Test
    public void productionRejectsBeforeChangingCoordinatesOrCallingRender() {
        BaniraPlatforms.install(new TestBaniraPlatform().development(false));
        double x = screen.inputState().mouseX(), y = screen.inputState().mouseY();
        assertThrows(IllegalStateException.class, () -> screen.runDevRenderAt(12, 34,
                () -> fail("production callback must not run")));
        assertPosition(x, y);
    }

    @Test
    public void nullCallbackDoesNotChangeCoordinates() {
        double x = screen.inputState().mouseX(), y = screen.inputState().mouseY();
        assertThrows(NullPointerException.class, () -> screen.runDevRenderAt(12, 34, null));
        assertPosition(x, y);
    }

    private void assertPosition(double x, double y) {
        assertEquals(x, screen.inputState().mouseX(), 0);
        assertEquals(y, screen.inputState().mouseY(), 0);
    }
}
