package xin.vanilla.banira.api.client.hud;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
