package xin.vanilla.banira.internal.client;

import org.junit.Test;
import xin.vanilla.banira.api.client.hud.BaniraHudBounds;

import static org.junit.Assert.assertEquals;

public class BaniraHudGeometryTest {

    @Test
    public void computesVanillaExperienceBarBounds() {
        BaniraHudBounds bounds = BaniraHudGeometry.experienceBarBounds(69, 180);

        assertEquals(69, bounds.x());
        assertEquals(151, bounds.y());
        assertEquals(182, bounds.width());
        assertEquals(5, bounds.height());
    }

    @Test
    public void computesVanillaExperienceTextBounds() {
        BaniraHudBounds bounds = BaniraHudGeometry.experienceTextBounds(69, 180);

        assertEquals(69, bounds.x());
        assertEquals(145, bounds.y());
        assertEquals(182, bounds.width());
        assertEquals(9, bounds.height());
    }
}
