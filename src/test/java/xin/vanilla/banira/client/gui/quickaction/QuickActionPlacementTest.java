package xin.vanilla.banira.client.gui.quickaction;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class QuickActionPlacementTest {
    @Test
    public void keepsPreferredPositionWhenItIsAvailable() {
        QuickActionRect result = QuickActionPlacement.resolve(
                10, 12, 20, 16, 100, 80, Collections.emptyList(), 4);

        assertEquals(new QuickActionRect(10, 12, 20, 16), result);
    }

    @Test
    public void movesToTheNearestAvailableEdgeOfAnObstacle() {
        QuickActionRect obstacle = new QuickActionRect(8, 8, 30, 30);

        QuickActionRect result = QuickActionPlacement.resolve(
                10, 10, 10, 10, 100, 100,
                Collections.singletonList(obstacle), 2);

        assertEquals(new QuickActionRect(10, 40, 10, 10), result);
        assertFalse(result.intersects(obstacle, 2));
    }

    @Test
    public void clampsThePreferredPositionInsideTheScreenMargin() {
        QuickActionRect result = QuickActionPlacement.resolve(
                -10, 95, 20, 20, 100, 100, Collections.emptyList(), 4);

        assertEquals(new QuickActionRect(4, 76, 20, 20), result);
    }

    @Test
    public void fallsBackToTheClampedPositionWhenNoAreaCanFit() {
        QuickActionRect result = QuickActionPlacement.resolve(
                10, 10, 20, 20, 50, 50,
                Collections.singletonList(new QuickActionRect(0, 0, 50, 50)), 4);

        assertEquals(new QuickActionRect(10, 10, 20, 20), result);
    }
}
