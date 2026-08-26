package xin.vanilla.banira.client.gui.tooltip;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TooltipPlacementTest {

    @Test
    public void centersTheTooltipAboveThePointer() {
        TooltipBounds bounds = TooltipPlacement.place(
                100, 100, 40, 20,
                240, 180, 4, 4, 4, 4);

        assertEquals(new TooltipBounds(80, 75, 40, 20), bounds);
        assertEquals(100.0D, bounds.x() + bounds.width() / 2.0D, 0.0001D);
        assertTrue(bounds.y() + bounds.height() < 100);
    }

    @Test
    public void keepsCenteredPlacementUntilTheScreenEdgeRequiresClamping() {
        TooltipBounds bounds = TooltipPlacement.place(
                230, 100, 50, 20,
                240, 180, 4, 4, 4, 4);

        assertEquals(new TooltipBounds(186, 75, 50, 20), bounds);
    }

    @Test
    public void flipsBelowWhenThereIsNoRoomAbove() {
        TooltipBounds bounds = TooltipPlacement.place(
                100, 12, 40, 30,
                240, 180, 4, 4, 4, 4);

        assertEquals(new TooltipBounds(80, 18, 40, 30), bounds);
        assertTrue(bounds.y() > 12);
    }

    @Test
    public void clampsOversizedContentInsideTheScreen() {
        TooltipBounds bounds = TooltipPlacement.place(
                30, 30, 232, 172,
                240, 180, 4, 4, 4, 4);

        assertEquals(new TooltipBounds(4, 4, 232, 172), bounds);
    }
}
