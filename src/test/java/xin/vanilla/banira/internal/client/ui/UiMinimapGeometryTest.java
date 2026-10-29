package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.internal.client.minimap.MinimapLineLayout;

import static org.junit.Assert.*;

public class UiMinimapGeometryTest {
    Object viewport(String name, Class<?>[] types, Object... args) throws Exception {
        try {
            return UiMinimapHostContractTest.call(name, types, args);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Viewport-aware minimap projection missing", missing);
        }
    }

    @Test
    public void journeyPixelProjectionConvertsToGuiOnceAndRetainsDepth() throws Exception {
        var projection = new org.joml.Matrix4f().setOrtho(0, 854, 480, 0, -1000, 1000);
        var pose = new org.joml.Matrix4f().translation(0, 0, 4);
        var guiPose = (org.joml.Matrix4f) viewport("guiPose",
                new Class[]{org.joml.Matrix4f.class, org.joml.Matrix4f.class, int.class, int.class}, pose, projection, 427, 240);
        var bounds = MinimapUiRows.project(guiPose, new ScreenCoordinate(750, 204, 51, 9), 427, 240);
        assertNotNull(bounds);
        assertEquals(375, bounds.x(), .001);
        assertEquals(102, bounds.y(), .001);
        assertEquals(25.5, bounds.width(), .001);
        assertEquals(4.5, bounds.height(), .001);
        assertEquals(4, guiPose.m32(), .001);
        var guiProjection = (org.joml.Matrix4f) viewport("guiProjection",
                new Class[]{org.joml.Matrix4f.class, int.class, int.class}, projection, 427, 240);
        assertEquals(2f / 427, guiProjection.m00(), .00001);
        assertEquals(-2f / 240, guiProjection.m11(), .00001);
        assertEquals(projection.m22(), guiProjection.m22(), 0);
        assertEquals(projection.m32(), guiProjection.m32(), 0);
        assertEquals(2f / 854, projection.m00(), .00001);
    }

    @Test
    public void guiProjectionKeepsExistingGuiPoseAndRejectsPerspective() throws Exception {
        var projection = new org.joml.Matrix4f().setOrtho(0, 427, 240, 0, -1000, 1000);
        var pose = new org.joml.Matrix4f().translation(100, 50, 2).scale(.75f, .75f, 1);
        var types = new Class[]{org.joml.Matrix4f.class, org.joml.Matrix4f.class, int.class, int.class};
        var result = (org.joml.Matrix4f) viewport("guiPose", types, pose, projection, 427, 240);
        assertTrue(pose.equals(result, .00001f));
        assertNull(viewport("guiPose", types, pose, new org.joml.Matrix4f().perspective(1, 1, .1f, 100), 427, 240));
        assertNull(viewport("guiPose", types, pose, projection, 0, 240));
        assertNull(viewport("guiPose", types, pose, new org.joml.Matrix4f(projection).m00(Float.NaN), 427, 240));
    }

    ScreenCoordinate fit(ScreenCoordinate measured, ScreenCoordinate reserved) throws Exception {
        return (ScreenCoordinate) UiMinimapHostContractTest.call("fit", new Class[]{ScreenCoordinate.class, ScreenCoordinate.class}, measured, reserved);
    }

    @Test
    public void actualAuiMeasurementFitsNativeSlotWithoutChangingNativeHeight() throws Exception {
        ScreenCoordinate fit = fit(new ScreenCoordinate(0, 0, 240, 24), new ScreenCoordinate(100, 50, 80, 9));
        assertEquals(80, fit.width(), .001);
        assertEquals(8, fit.height(), .001);
        assertEquals(100, fit.x(), .001);
        assertEquals(50, fit.y(), .001);
    }

    @Test
    public void shortLabelsStayCenteredAndInvalidGeometryNeverClaims() throws Exception {
        ScreenCoordinate fit = fit(new ScreenCoordinate(0, 0, 20, 9), new ScreenCoordinate(10, 20, 80, 11));
        assertEquals(40, fit.x(), .001);
        assertEquals(20, fit.y(), .001);
        assertNull(fit(new ScreenCoordinate(0, 0, Double.NaN, 9), new ScreenCoordinate(0, 0, 80, 11)));
        assertNull(fit(new ScreenCoordinate(0, 0, 20, 0), new ScreenCoordinate(0, 0, 80, 11)));
        assertNull(fit(new ScreenCoordinate(0, 0, 20, 9), new ScreenCoordinate(0, 0, -1, 11)));
    }

    @Test
    public void originalTwoRowCornersAndAboveOrderRemainInsideScreen() {
        for (float y : new float[]{0, 12, 80, 99})
            for (boolean above : new boolean[]{false, true}) {
                float[] p = MinimapLineLayout.positions(y, new float[]{9, 18}, 100, above);
                assertTrue(p[0] >= 0);
                assertTrue(p[1] >= p[0] + 11);
                assertTrue(p[1] + 18 <= 100);
            }
    }

    @Test
    public void hostTranslationAndFontScaleAreProjectedExactlyOnce() {
        var pose = new org.joml.Matrix4f().translation(100, 50, .5f).scale(.75f, .75f, 1);
        var bounds = MinimapUiRows.project(pose, new ScreenCoordinate(-20, 10, 40, 9), 320, 180);
        assertNotNull(bounds);
        assertEquals(85, bounds.x(), .001);
        assertEquals(57.5, bounds.y(), .001);
        assertEquals(30, bounds.width(), .001);
        assertEquals(6.75, bounds.height(), .001);
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().rotateZ(.1f), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().translate(310, 0, 0), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
    }

    @Test
    public void nonfinitePoseNeverClaimsAuiOwnership() {
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().m01(Float.NaN), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
        assertNull(MinimapUiRows.project(new org.joml.Matrix4f().m22(Float.NaN), new ScreenCoordinate(0, 0, 40, 9), 320, 180));
    }
}
