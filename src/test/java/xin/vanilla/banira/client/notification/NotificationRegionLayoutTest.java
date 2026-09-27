package xin.vanilla.banira.client.notification;

import org.junit.Test;
import xin.vanilla.banira.common.enums.EnumPosition;

import static org.junit.Assert.*;

public class NotificationRegionLayoutTest {
    @Test
    public void visibleBubblesKeepCapacityBeforeNewOtherRegionArrivals() {
        assertEquals(java.util.Arrays.asList("bottom-active", "top-waiting", "bottom-waiting"),
                NotificationRegionLayout.admissionOrder(java.util.Arrays.asList("top-waiting", "bottom-active", "bottom-waiting"),
                        name -> name.endsWith("active")));
    }

    @Test
    public void movingBubbleReservesItsCurrentPositionWhileSettling() {
        NotificationRegionLayout layout = new NotificationRegionLayout(5);
        NotificationRegionLayout.Rect area = new NotificationRegionLayout.Rect(0, 0, 100, 100);
        assertEquals(0, layout.place(EnumPosition.TOP_LEFT, area, 80, 20, 4, 5).y);
        layout.follow(EnumPosition.TOP_LEFT, area, new NotificationRegionLayout.Rect(0, 30, 80, 20), 4);
        assertEquals(54, layout.place(EnumPosition.TOP_LEFT, area, 80, 20, 4, 5).y);
        assertNull(layout.place(EnumPosition.LEFT_CENTER, new NotificationRegionLayout.Rect(0, 35, 100, 30), 80, 20, 4, 5));
    }

    @Test
    public void anchorsStayWithinScreen() {
        for (EnumPosition position : EnumPosition.values()) {
            NotificationRegionLayout.Rect area = NotificationRegionLayout.region(position, 400, 240, 30, 32, 6);
            assertTrue(area.x >= 6 && area.y >= 6);
            assertTrue(area.x + area.width <= 394 && area.y + area.height <= 234);
        }
    }

    @Test
    public void topAndBottomStackTowardInterior() {
        NotificationRegionLayout layout = new NotificationRegionLayout(6);
        NotificationRegionLayout.Rect top = new NotificationRegionLayout.Rect(0, 0, 120, 100);
        assertEquals(0, layout.place(EnumPosition.TOP_LEFT, top, 70, 20, 4, 3).y);
        assertEquals(24, layout.place(EnumPosition.TOP_LEFT, top, 70, 20, 4, 3).y);
        NotificationRegionLayout.Rect bottom = new NotificationRegionLayout.Rect(200, 100, 120, 100);
        assertEquals(180, layout.place(EnumPosition.BOTTOM_RIGHT, bottom, 70, 20, 4, 3).y);
        assertEquals(156, layout.place(EnumPosition.BOTTOM_RIGHT, bottom, 70, 20, 4, 3).y);
    }

    @Test
    public void rejectsOverflowAndOverlappingRegionsAndHonorsLimits() {
        NotificationRegionLayout layout = new NotificationRegionLayout(2);
        NotificationRegionLayout.Rect area = new NotificationRegionLayout.Rect(0, 0, 100, 100);
        assertNull(layout.place(EnumPosition.TOP_LEFT, area, 101, 20, 2, 1));
        assertNotNull(layout.place(EnumPosition.TOP_LEFT, area, 100, 20, 2, 1));
        assertNull(layout.place(EnumPosition.TOP_LEFT, area, 100, 20, 2, 1));
        assertNull(layout.place(EnumPosition.TOP_CENTER, area, 100, 20, 2, 1));
        assertNotNull(layout.place(EnumPosition.BOTTOM_LEFT, area, 100, 20, 2, 1));
        assertNull(layout.place(EnumPosition.BOTTOM_RIGHT, new NotificationRegionLayout.Rect(200, 0, 100, 100), 50, 20, 2, 1));
    }

    @Test
    public void fullRegionDefersNextBubble() {
        NotificationRegionLayout layout = new NotificationRegionLayout(10);
        NotificationRegionLayout.Rect area = new NotificationRegionLayout.Rect(0, 0, 100, 42);
        assertNotNull(layout.place(EnumPosition.TOP_LEFT, area, 50, 20, 3, 10));
        assertNull(layout.place(EnumPosition.TOP_LEFT, area, 50, 20, 3, 10));
    }
}
