package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiGameElement;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class UiGamePlacementTest {
    @org.junit.BeforeClass
    public static void bootstrap() {
        UiDomControlsTest.bootstrap();
    }

    @org.junit.Before
    public void platform() {
        new UiDomControlsTest().platform();
    }

    @Test
    public void publicPlacementContractExists() {
        boolean found = java.util.Arrays.stream(UiView.class.getMethods()).anyMatch(method ->
                method.getName().equals("gameElement") && java.util.Arrays.equals(method.getParameterTypes(),
                        new Class<?>[]{String.class, UiGameElement.class, ScreenCoordinate.class}));
        assertTrue("Animated icons need finite local placement through the public API", found);
    }

    @Test
    public void placementRetainsLocalGeometryAndSurvivesOtherUpdates() throws Exception {
        UiDomControlsTest.Dom dom = new UiDomControlsTest.Dom();
        NativeUiView view = dom.view();
        var area = new ScreenCoordinate(2.5, 7, 16, 20);
        view.gameElement("text", null, area);
        assertTrue(dom.ids.get("text").attributes.get("style").contains("left:2.5px"));
        view.text("text", xin.vanilla.banira.BaniraComponent.get().literal("?"));
        assertTrue(dom.ids.get("text").attributes.get("style").contains("height:20.0px"));
        view.gameElement("text", null, new ScreenCoordinate(0, 0, 0, 0));
        assertTrue(dom.ids.get("text").attributes.get("style").contains("display:none"));
        view.close();
        assertTrue(view.gameAreas.isEmpty());
    }

    @Test
    public void nonfiniteAndNegativeGeometryAreRejectedBeforeRetention() {
        NativeUiView view = new NativeUiView(new Object());
        for (ScreenCoordinate area : new ScreenCoordinate[]{new ScreenCoordinate(Double.NaN, 0, 1, 1),
                new ScreenCoordinate(0, Double.POSITIVE_INFINITY, 1, 1), new ScreenCoordinate(0, 0, -1, 1)}) {
            assertThrows(IllegalArgumentException.class, () -> view.gameElement("icon", null, area));
        }
        assertTrue(view.gameAreas.isEmpty());
    }
}
