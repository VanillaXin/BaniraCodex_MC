package xin.vanilla.banira.api.client.event;

import org.junit.Test;

import static org.junit.Assert.*;

public class BaniraScreenInfoTest {

    @Test
    public void closedScreenHasStableEmptyValues() {
        BaniraScreenInfo screen = BaniraScreenInfo.closed();

        assertFalse(screen.open());
        assertEquals("", screen.title());
        assertEquals(0, screen.width());
        assertEquals(0, screen.height());
    }

    @Test
    public void normalizesNullableTitleAndNegativeSize() {
        BaniraScreenInfo screen = new BaniraScreenInfo("test.Screen", null, -1, -2, true);

        assertTrue(screen.open());
        assertEquals("", screen.title());
        assertEquals(0, screen.width());
        assertEquals(0, screen.height());
        assertTrue(screen.matchesClassName("test.Screen"));
    }
}
