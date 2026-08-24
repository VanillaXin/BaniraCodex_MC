package xin.vanilla.banira.client.gui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import static org.junit.Assert.assertEquals;

public class PlayerConfigActionBarLayoutTest {

    @Test
    public void centersButtonsInsideTheSameSplitZonesAsTheConfigEditor() {
        ScreenCoordinate[] buttons = PlayerConfigActionBarLayout.equalSplitButtons(
                10, 10, 407, 220, 10, 18, 1);

        assertEquals(15, buttons[0].xInt());
        assertEquals(193, buttons[0].widthInt());
        assertEquals(219, buttons[1].xInt());
        assertEquals(193, buttons[1].widthInt());
        assertEquals(207, buttons[0].yInt());
        assertEquals(18, buttons[0].heightInt());
    }
}
