package xin.vanilla.banira.client.gui.tooltip;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TooltipRequestCollectorTest {

    @Test
    public void onlyLastSubmittedRequestWinsTheFrame() {
        TooltipRequestCollector<String> collector = new TooltipRequestCollector<>();
        Object screen = new Object();
        collector.beginFrame(screen);
        collector.submit("bottom");
        collector.submit("top");

        assertTrue(collector.hasWinner());
        assertEquals("top", collector.winner());
    }

    @Test
    public void changingScreenClearsRequestsFromPreviousScreen() {
        TooltipRequestCollector<String> collector = new TooltipRequestCollector<>();
        collector.beginFrame(new Object());
        collector.submit("old");

        collector.beginFrame(new Object());

        assertFalse(collector.hasWinner());
    }
}
