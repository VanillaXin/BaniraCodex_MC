package xin.vanilla.banira.client.gui;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.PreeditEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.api.client.input.BaniraInputState;
import xin.vanilla.banira.client.gui.widget.InputWidget;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.platform.TestBaniraPlatform;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BaniraScreenDevRenderTest {
    @org.junit.Rule
    public final xin.vanilla.banira.test.NativeClientRule nativeClient = new xin.vanilla.banira.test.NativeClientRule();

    private BaniraScreen screen;
    private final List<Boolean> textInputFocus = new ArrayList<>();

    @Before
    public void setUp() {
        BaniraPlatforms.install(new TestBaniraPlatform().development(true));
        screen = new BaniraScreen(net.minecraft.network.chat.Component.literal("dev render")) {
            @Override
            protected void onRender(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
            }

            @Override
            void updateNativeTextInputFocus(boolean focused) {
                textInputFocus.add(focused);
            }
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

    @Test
    public void customFocusRoutesPreeditWithoutCommittingItAndCommitsChineseOnce() {
        var input = new InputWidget(screen);
        var preedit = new PreeditEvent("zhong", 5, List.of("zhong"), 0);
        assertFalse(screen.preeditUpdated(preedit));
        assertTrue(screen.requestFocus(input));
        assertTrue(screen.requestFocus(input));
        assertEquals(List.of(true), textInputFocus);
        assertTrue(screen.preeditUpdated(preedit));
        assertEquals("", input.value());
        assertTrue(screen.charTyped(new CharacterEvent('\u4e2d')));
        assertTrue(screen.preeditUpdated(null));
        assertEquals("\u4e2d", input.value());
        screen.unfocusWidget(input);
        assertEquals(List.of(true, false), textInputFocus);
        assertFalse(screen.preeditUpdated(preedit));
    }

    @Test
    public void switchingAndClearingWidgetsReleaseTheirComposition() {
        var events = new ArrayList<PreeditEvent>();
        var first = new InputWidget(screen) {
            @Override
            public void updatePreedit(PreeditEvent event) {
                super.updatePreedit(event);
                events.add(event);
            }
        };
        var second = new InputWidget(screen);
        var preedit = new PreeditEvent("ni", 2, List.of("ni"), 0);
        assertTrue(screen.requestFocus(first));
        assertTrue(screen.preeditUpdated(preedit));
        assertTrue(screen.requestFocus(second));
        assertEquals(2, events.size());
        assertSame(preedit, events.getFirst());
        assertNull(events.getLast());
        screen.clearWidgets();
        assertEquals(List.of(true, true, false), textInputFocus);
        assertFalse(screen.preeditUpdated(preedit));
    }

    private void assertPosition(double x, double y) {
        assertEquals(x, screen.inputState().mouseX(), 0);
        assertEquals(y, screen.inputState().mouseY(), 0);
    }
}
