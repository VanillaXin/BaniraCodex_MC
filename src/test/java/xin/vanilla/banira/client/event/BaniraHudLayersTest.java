package xin.vanilla.banira.client.event;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class BaniraHudLayersTest {

    @Test
    public void replaceCancelsVanillaAndCanUnregister() {
        AtomicInteger calls = new AtomicInteger();
        BaniraHudLayers.Registration registration = BaniraHudLayers.replace(
                BaniraHudOverlayElement.EXPERIENCE_BAR,
                event -> calls.incrementAndGet()
        );

        BaniraHudRenderEvent first = new BaniraHudRenderEvent(BaniraHudOverlayElement.EXPERIENCE_BAR, null, true);
        BaniraClientEventHub.dispatchHudPreRender(first);

        assertEquals(1, calls.get());
        assertTrue(first.canceled());

        registration.unregister();
        BaniraHudRenderEvent second = new BaniraHudRenderEvent(BaniraHudOverlayElement.EXPERIENCE_BAR, null, true);
        BaniraClientEventHub.dispatchHudPreRender(second);

        assertEquals(1, calls.get());
        assertFalse(second.canceled());
    }
}
