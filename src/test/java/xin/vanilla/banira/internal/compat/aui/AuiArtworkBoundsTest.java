package xin.vanilla.banira.internal.compat.aui;

import org.junit.Test;
import xin.vanilla.banira.client.data.ScreenCoordinate;

import static org.junit.Assert.assertEquals;

public class AuiArtworkBoundsTest {
    @Test
    public void retainedArtworkIsClippedAroundTheTooltipAndInsideTheViewport() {
        ScreenCoordinate artwork = new ScreenCoordinate(8, 0, 60, 80);
        ScreenCoordinate viewport = new ScreenCoordinate(0, 12, 100, 50);
        assertEquals(java.util.Arrays.asList(
                new ScreenCoordinate(8, 12, 60, 8),
                new ScreenCoordinate(8, 40, 60, 22),
                new ScreenCoordinate(8, 20, 12, 20),
                new ScreenCoordinate(50, 20, 18, 20)
        ), AuiGameElements.uncovered(artwork, viewport, new ScreenCoordinate(20.5, 20.5, 29, 19)));
        assertEquals(java.util.Arrays.asList(), AuiGameElements.uncovered(artwork, viewport, new ScreenCoordinate(0, 0, 100, 100)));
        assertEquals(java.util.Arrays.asList(viewport), AuiGameElements.uncovered(artwork, viewport, new ScreenCoordinate(75, 12, 20, 40)));
    }

    @Test
    public void retainsBothLandscapeAndPortraitAspectRatiosInsideTheSlot() {
        ScreenCoordinate slot = new ScreenCoordinate(10, 20, 160, 100);
        ScreenCoordinate landscape = AuiGameElements.artworkBounds(slot, 320, 180);
        assertEquals(160, landscape.widthInt());
        assertEquals(90, landscape.heightInt());
        assertEquals(25, landscape.yInt());
        ScreenCoordinate portrait = AuiGameElements.artworkBounds(slot, 120, 240);
        assertEquals(50, portrait.widthInt());
        assertEquals(100, portrait.heightInt());
        assertEquals(65, portrait.xInt());
    }
}
