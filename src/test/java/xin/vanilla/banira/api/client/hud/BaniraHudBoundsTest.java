package xin.vanilla.banira.api.client.hud;

import org.junit.Test;

import static org.junit.Assert.*;

public class BaniraHudBoundsTest {

    @Test
    public void computesEdgesAndProgressPositions() {
        BaniraHudBounds bounds = BaniraHudBounds.of(10, 20, 182, 5);

        assertTrue(bounds.isKnown());
        assertEquals(192, bounds.right());
        assertEquals(25, bounds.bottom());
        assertEquals(101, bounds.centerX());
        assertEquals(22, bounds.centerY());
        assertEquals(10, bounds.progressX(-1.0f));
        assertEquals(101, bounds.progressX(0.5f));
        assertEquals(192, bounds.progressX(2.0f));
    }

    @Test
    public void transformsKnownBoundsOnly() {
        BaniraHudBounds bounds = BaniraHudBounds.of(10, 20, 30, 40);
        BaniraHudBounds offset = bounds.offset(2, 3);
        BaniraHudBounds inflated = bounds.inflate(4);

        assertEquals(12, offset.x());
        assertEquals(23, offset.y());
        assertEquals(6, inflated.x());
        assertEquals(16, inflated.y());
        assertEquals(38, inflated.width());
        assertEquals(48, inflated.height());
        assertSame(BaniraHudBounds.empty(), BaniraHudBounds.empty().offset(1, 1));
        assertSame(BaniraHudBounds.empty(), BaniraHudBounds.empty().inflate(1));
        assertFalse(BaniraHudBounds.empty().isKnown());
    }
}
