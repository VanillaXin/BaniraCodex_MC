package xin.vanilla.banira.client.gui.interaction;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HoverInsetsTest {

    @Test
    public void adjacentCellsMeetWithoutGapOrOverlap() {
        HoverInsets insets = HoverInsets.fromSpacing(3.0D, 2.0D);
        double firstRight = 10.0D + insets.right();
        double secondLeft = 13.0D - insets.left();

        assertEquals(firstRight, secondLeft, 0.0001D);
        assertFalse(insets.contains(firstRight, 5.0D, 0.0D, 0.0D, 10.0D, 10.0D));
        assertTrue(insets.contains(firstRight, 5.0D, 13.0D, 0.0D, 10.0D, 10.0D));
    }

    @Test
    public void contentInsideCellExpandsToSharedCellBoundary() {
        HoverInsets insets = HoverInsets.partitionCell(
                1.0D, 1.0D, 16.0D, 16.0D,
                20.0D, 20.0D, 1.0D, 1.0D);
        double firstRight = 1.0D + 16.0D + insets.right();
        double secondContentLeft = 21.0D + 1.0D;
        double secondLeft = secondContentLeft - insets.left();

        assertEquals(20.5D, firstRight, 0.0001D);
        assertEquals(firstRight, secondLeft, 0.0001D);
        assertTrue(insets.contains(0.0D, 0.0D, 1.0D, 1.0D, 16.0D, 16.0D));
    }
}
