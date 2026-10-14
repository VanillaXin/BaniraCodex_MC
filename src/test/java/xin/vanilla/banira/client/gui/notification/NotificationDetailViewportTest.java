package xin.vanilla.banira.client.gui.notification;

import org.junit.Test;

import static org.junit.Assert.*;

public class NotificationDetailViewportTest {
    @Test public void clippedLinesCannotBeClicked() {
        NotificationDetailViewport view = view(60);
        assertEquals(0, view.visibleLineAt(11, 21));
        assertEquals(-1, view.visibleLineAt(11, 80));
        assertEquals(-1, view.visibleLineAt(9, 21));
        assertEquals(-1, view.visibleLineAt(110, 21));
        view.scrollBy(3);
        assertEquals(3, view.visibleLineAt(11, 21));
        assertEquals(20, view.lineY(3));
    }

    @Test public void resizingClampsScrollWithoutLosingTheLastLine() {
        NotificationDetailViewport view = view(65);
        view.scrollBy(1000);
        assertEquals(99, view.visibleLineAt(11, 84));
        assertEquals(75, view.lineY(99));
        view.layout(10, 20, 100, 1200, 10, 100);
        assertEquals(0, view.visibleLineAt(11, 21));
        assertEquals(-1, view.visibleLineAt(11, 1100));
    }

    @Test public void resetReturnsToTheFirstLine() {
        NotificationDetailViewport view = view(60);
        view.scrollBy(3); view.reset();
        assertEquals(0, view.visibleLineAt(11, 21));
    }

    @Test public void partialTopLineUsesTheSameOffsetAsRendering() {
        NotificationDetailViewport view = view(65);
        view.scrollBy(1000);
        assertEquals(93, view.visibleLineAt(11, 20));
        assertEquals(15, view.lineY(93));
        assertEquals(94, view.visibleLineAt(11, 25));
    }

    @Test public void emptyAndZeroSizedAreasDoNotAcceptInput() {
        NotificationDetailViewport view = view(0);
        assertEquals(-1, view.visibleLineAt(11, 20));
        view.layout(10, 20, 0, 60, 10, 100);
        assertEquals(-1, view.visibleLineAt(10, 21));
        view.layout(10, 20, 100, 60, 10, 0);
        assertEquals(-1, view.visibleLineAt(11, 21));
    }

    @Test public void invalidScrollDeltaDoesNotCorruptTheViewport() {
        NotificationDetailViewport view = view(60);
        view.scrollBy(3); view.scrollBy(Double.NaN); view.scrollBy(Double.POSITIVE_INFINITY);
        assertEquals(3, view.visibleLineAt(11, 21));
        view.scrollBy(-1000);
        assertEquals(0, view.visibleLineAt(11, 21));
    }

    private static NotificationDetailViewport view(int height) {
        NotificationDetailViewport view = new NotificationDetailViewport();
        view.layout(10, 20, 100, height, 10, 100);
        return view;
    }
}
