package xin.vanilla.banira.api.client.hud;

import org.junit.Test;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.api.client.render.BaniraDrawHandle;

import javax.annotation.Nonnull;

import static org.junit.Assert.*;

public class BaniraHudRenderEventTest {

    @Test
    public void cancellablePreEventCanBeCanceled() {
        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                HudRenderPhase.PRE,
                HudOverlayElement.EXPERIENCE,
                context(),
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
                context(),
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
                context(),
                BaniraHudBounds.of(69, 151, 182, 5),
                false
        );

        assertTrue(event.hasKnownBounds());
        assertTrue(event.isExperience());
        assertEquals(69, event.bounds().x());
    }

    private static BaniraHudRenderContext context() {
        return new BaniraHudRenderContext(new BaniraDrawContext(new NoopDrawHandle(), 320, 180, 0.5f), 320, 180, 0.5f);
    }

    private static final class NoopDrawHandle implements BaniraDrawHandle {
        @Override
        public void fill(int x, int y, int width, int height, int argb) {
        }

        @Override
        public void line(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        }

        @Override
        public void roundedRect(int x, int y, int width, int height, int argb, int radius) {
        }

        @Override
        public void text(@Nonnull String text, int x, int y, int argb, boolean shadow) {
        }

        @Override
        public void texture(@Nonnull String textureId, int x, int y, int width, int height,
                            float u, float v, int textureWidth, int textureHeight) {
        }
    }
}
