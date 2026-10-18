package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.internal.client.minimap.MinimapLineLayout;
import static org.junit.Assert.*;

public class UiMinimapGeometryTest {
    ScreenCoordinate fit(ScreenCoordinate measured, ScreenCoordinate reserved) throws Exception {
        return (ScreenCoordinate) UiMinimapHostContractTest.call("fit", new Class[]{ScreenCoordinate.class, ScreenCoordinate.class}, measured, reserved);
    }
    @Test public void actualAuiMeasurementFitsNativeSlotWithoutChangingNativeHeight() throws Exception {
        ScreenCoordinate fit = fit(new ScreenCoordinate(0, 0, 240, 24), new ScreenCoordinate(100, 50, 80, 9));
        assertEquals(80, fit.width(), .001); assertEquals(8, fit.height(), .001);
        assertEquals(100, fit.x(), .001); assertEquals(50, fit.y(), .001);
    }
    @Test public void shortLabelsStayCenteredAndInvalidGeometryNeverClaims() throws Exception {
        ScreenCoordinate fit = fit(new ScreenCoordinate(0, 0, 20, 9), new ScreenCoordinate(10, 20, 80, 11));
        assertEquals(40, fit.x(), .001); assertEquals(20, fit.y(), .001);
        assertNull(fit(new ScreenCoordinate(0, 0, Double.NaN, 9), new ScreenCoordinate(0, 0, 80, 11)));
        assertNull(fit(new ScreenCoordinate(0, 0, 20, 0), new ScreenCoordinate(0, 0, 80, 11)));
        assertNull(fit(new ScreenCoordinate(0, 0, 20, 9), new ScreenCoordinate(0, 0, -1, 11)));
    }
    @Test public void originalTwoRowCornersAndAboveOrderRemainInsideScreen() {
        for (float y : new float[]{0, 12, 80, 99}) for (boolean above : new boolean[]{false, true}) {
            float[] p = MinimapLineLayout.positions(y, new float[]{9, 18}, 100, above);
            assertTrue(p[0] >= 0); assertTrue(p[1] >= p[0] + 11); assertTrue(p[1] + 18 <= 100);
        }
    }
    @Test public void hostTranslationAndFontScaleAreProjectedExactlyOnce() {
        var pose = new org.joml.Matrix4f().translation(100, 50, .5f).scale(.75f, .75f, 1);
        var bounds = MinimapUiRows.project(pose, new ScreenCoordinate(-20, 10, 40, 9), 320, 180);
        assertNotNull(bounds); assertEquals(85, bounds.x(), .001); assertEquals(57.5, bounds.y(), .001);
        assertEquals(30, bounds.width(), .001); assertEquals(6.75, bounds.height(), .001);
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().rotateZ(.1f), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().translate(310, 0, 0), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
    }
    @Test public void nonfinitePoseNeverClaimsAuiOwnership() {
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().m01(Float.NaN), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().m22(Float.NaN), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
    }
}
