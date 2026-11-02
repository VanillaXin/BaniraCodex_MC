package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import java.util.Arrays;

import static org.junit.Assert.*;

public class UiPopupLayoutTest {
    private ScreenCoordinate place(ScreenCoordinate anchor, ScreenCoordinate viewport, int options) throws Exception {
        lombok.var method = Arrays.stream(UiSelectPopup.class.getDeclaredMethods()).filter(value -> value.getName().equals("placement")).findFirst().orElse(null);
        assertNotNull("Dropdown geometry must follow its control and option count", method);
        return (ScreenCoordinate) method.invoke(null, anchor, viewport, options);
    }

    @Test
    public void shortMenuFitsAboveAControlNearTheBottom() throws Exception {
        lombok.var box = place(new ScreenCoordinate(20, 160, 180, 22), new ScreenCoordinate(0, 0, 427, 240), 2);
        assertEquals(20, box.x(), 0);
        assertEquals(72, box.y(), 0);
        assertEquals(180, box.width(), 0);
        assertEquals(84, box.height(), 0);
    }

    @Test
    public void largeMenusCapVisibleRowsAndRemainInsideTheViewport() throws Exception {
        lombok.var box = place(new ScreenCoordinate(20, 20, 180, 22), new ScreenCoordinate(0, 0, 427, 600), 1000);
        assertEquals(216, box.height(), 0);
        assertEquals(46, box.y(), 0);
        box = place(new ScreenCoordinate(20, 20, 180, 22), new ScreenCoordinate(0, 0, 427, 240), 1000);
        assertTrue(box.y() + box.height() <= 232);
    }

    @Test
    public void rightEdgeAndTinyViewportsKeepTheWholeMenuVisible() throws Exception {
        lombok.var box = place(new ScreenCoordinate(390, 100, 80, 22), new ScreenCoordinate(0, 0, 427, 240), 2);
        assertEquals(299, box.x(), 0);
        assertEquals(120, box.width(), 0);
        box = place(new ScreenCoordinate(10, 28, 70, 22), new ScreenCoordinate(0, 0, 100, 80), 2);
        assertEquals(8, box.x(), 0);
        assertEquals(8, box.y(), 0);
        assertEquals(84, box.width(), 0);
        assertEquals(64, box.height(), 0);
    }

    @Test
    public void emptyMenusStillLeaveRoomForSearch() throws Exception {
        assertEquals(62, place(new ScreenCoordinate(20, 20, 180, 22), new ScreenCoordinate(0, 0, 427, 240), 0).height(), 0);
    }

    @Test
    public void originalResourceBudgetsRemainBinding() throws Exception {
        assertEquals(com.google.common.collect.ImmutableList.of(), UiBoundaryChecks.resources(UiBoundaryChecks.ownResources("banira_codex"), true));
    }
}
