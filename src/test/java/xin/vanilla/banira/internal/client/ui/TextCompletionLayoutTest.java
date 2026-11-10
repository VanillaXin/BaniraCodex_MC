package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import static org.junit.Assert.*;

public class TextCompletionLayoutTest {
    @Test
    public void reducesVisibleRowsInsteadOfCoveringTheInput() {
        ScreenCoordinate anchor = new ScreenCoordinate(10, 100, 200, 16);
        ScreenCoordinate menu = TextCompletionLayout.bounds(anchor, new ScreenCoordinate(0, 0, 320, 240), 64, 16);
        assertTrue(menu.y() >= anchor.y() + anchor.height());
        assertTrue(menu.y() + menu.height() <= 236);
        assertEquals(6, ((int) menu.height() - 8) / 16);
    }

    @Test
    public void opensAboveNearTheBottomAndKeepsRightEdgeVisible() {
        ScreenCoordinate anchor = new ScreenCoordinate(300, 210, 200, 16);
        ScreenCoordinate menu = TextCompletionLayout.bounds(anchor, new ScreenCoordinate(0, 0, 320, 240), 64, 22);
        assertTrue(menu.y() + menu.height() <= anchor.y());
        assertTrue(menu.x() >= 4);
        assertTrue(menu.x() + menu.width() <= 316);
    }

    @Test
    public void hidesIfNeitherSideCanFitARow() {
        ScreenCoordinate menu = TextCompletionLayout.bounds(new ScreenCoordinate(4, 10, 20, 20),
                new ScreenCoordinate(0, 0, 50, 40), 5, 22);
        assertEquals(0, menu.height(), 0);
    }
}
