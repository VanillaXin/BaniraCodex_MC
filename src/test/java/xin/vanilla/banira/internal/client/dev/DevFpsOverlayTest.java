package xin.vanilla.banira.internal.client.dev;

import org.junit.Test;

import static org.junit.Assert.*;

public class DevFpsOverlayTest {

    @Test
    public void rendersOnlyForVisibleDevelopmentHud() {
        assertTrue(DevFpsOverlay.shouldRender(true, false, false));
        assertFalse(DevFpsOverlay.shouldRender(false, false, false));
        assertFalse(DevFpsOverlay.shouldRender(true, true, false));
        assertFalse(DevFpsOverlay.shouldRender(true, false, true));
    }

    @Test
    public void formatsVanillaFpsWithoutAdditionalSampling() {
        assertEquals("FPS: 144", DevFpsOverlay.format(144));
        assertEquals("FPS: 0", DevFpsOverlay.format(-1));
    }
}
