package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import static org.junit.Assert.*;

public class UiPointerStateTest {
    private BaniraColorConfig colors(int background) {
        return new BaniraColorConfig().bgSurface(background).cursorLightMainOverride(11).cursorDarkMainOverride(22)
                .cursorLightPressedOverride(33).cursorDarkPressedOverride(44);
    }
    private HudDrawList frame(UiPointerState state) { return state.frame(colors(0xFFFFFFFF), 20, 30, 999); }

    @Test public void originalNineBoxesAndDepthArePreserved() {
        var parts = frame(new UiPointerState()).parts();
        int[][] expected = {{20,30,1,1},{16,32,3,1},{18,32,1,3},{16,28,3,1},{18,26,1,3},
                {22,32,3,1},{22,32,1,3},{22,28,3,1},{22,26,1,3}};
        assertEquals(9, parts.size());
        for (int i = 0; i < 9; i++) {
            var p = parts.get(i);
            assertEquals(expected[i][0], p.x, 0); assertEquals(expected[i][1], p.y, 0);
            assertEquals(expected[i][2], p.width, 0); assertEquals(expected[i][3], p.height, 0);
            assertEquals(11, p.color); assertEquals(999, p.depth, 0);
        }
    }
    @Test public void buttonsAreIndependentAndRepeatedEventsAreIdempotent() {
        UiPointerState state = new UiPointerState();
        state.button(0, true); state.button(0, true); state.button(2, true);
        var p = frame(state).parts();
        assertEquals(33, p.get(0).color); assertEquals(33, p.get(1).color); assertEquals(11, p.get(5).color);
        state.button(0, false); state.button(0, false); state.button(99, true);
        p = frame(state).parts(); assertEquals(33, p.get(0).color); assertEquals(11, p.get(1).color);
        state.button(1, true); assertEquals(33, frame(state).parts().get(5).color);
    }
    @Test public void darkPaletteUsesItsPressedColor() {
        UiPointerState state = new UiPointerState(); state.button(1, true);
        var p = state.frame(colors(0xFF000000), 20, 30, 1).parts();
        assertEquals(22, p.get(0).color); assertEquals(44, p.get(5).color);
    }
    @Test public void scrollKeepsOriginalClampLineAndDecay() {
        UiPointerState state = new UiPointerState(); state.scroll(100); state.scroll(100);
        var p = frame(state).parts(); assertEquals(10, p.size()); assertEquals(35, p.get(0).y, 0);
        assertEquals(26, p.get(9).x, 0); assertEquals(30, p.get(9).y, 0); assertEquals(6, p.get(9).height, 0);
        for (int i = 0; i < 20; i++) p = frame(state).parts();
        assertEquals(9, p.size()); assertEquals(30, p.get(0).y, 0);
    }
    @Test public void resetDropsButtonsAndScroll() {
        UiPointerState state = new UiPointerState(); state.button(0, true); state.scroll(-3); state.reset();
        var p = frame(state).parts(); assertEquals(9, p.size()); assertEquals(30, p.get(0).y, 0); assertEquals(11, p.get(1).color);
    }
    @Test public void customMainColorsRetainThemePressedColors() {
        UiPointerState state = new UiPointerState(); state.colors(55, 66); state.button(0, true);
        var p = frame(state).parts(); assertEquals(55, p.get(0).color); assertEquals(33, p.get(1).color);
        state.themeChanged(); assertEquals(66, state.frame(colors(0xFF000000), 0, 0, 1).parts().get(0).color);
    }
    @Test public void paletteRefreshesOnTenthFrameOrThemeInvalidation() {
        UiPointerState state = new UiPointerState(); frame(state);
        for (int i = 0; i < 9; i++) assertEquals(11, state.frame(colors(0xFF000000), 0, 0, 1).parts().get(0).color);
        assertEquals(22, state.frame(colors(0xFF000000), 0, 0, 1).parts().get(0).color);
        state.themeChanged(); assertEquals(11, frame(state).parts().get(0).color);
    }
    @Test public void invalidScrollDoesNotPoisonCoordinates() {
        UiPointerState state = new UiPointerState(); state.scroll(Double.NaN); state.scroll(Double.POSITIVE_INFINITY);
        assertEquals(9, frame(state).parts().size());
    }
}
