package xin.vanilla.banira.api.client.hud;

import org.junit.Test;

import static org.junit.Assert.*;

public class BaniraHudRenderEventTest {

    @Test
    public void cancellablePreEventCanBeCanceled() {
        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                HudRenderPhase.PRE,
                HudOverlayElement.EXPERIENCE,
                new BaniraHudRenderContext(new Object(), 320, 180, 0.5f),
                true
        );

        event.cancel();

        assertTrue(event.canceled());
        assertFalse(event.bounds().isKnown());
        assertTrue(event.isPre());
        assertFalse(event.isPost());
        assertTrue(event.isExperience());
        assertFalse(event.hasKnownBounds());
    }

    @Test
    public void nonCancellableEventIgnoresCancel() {
        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                HudRenderPhase.POST,
                HudOverlayElement.ALL,
                new BaniraHudRenderContext(new Object(), 320, 180, 0.5f),
                false
        );

        event.cancel();

        assertFalse(event.canceled());
    }

    @Test
    public void reportsKnownBoundsWhenAdapterProvidesThem() {
        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                HudRenderPhase.POST,
                HudOverlayElement.EXPERIENCE_BAR,
                new BaniraHudRenderContext(new Object(), 320, 180, 0.5f),
                BaniraHudBounds.of(69, 151, 182, 5),
                false
        );

        assertTrue(event.hasKnownBounds());
        assertTrue(event.isExperience());
        assertEquals(69, event.bounds().x());
    }
}
