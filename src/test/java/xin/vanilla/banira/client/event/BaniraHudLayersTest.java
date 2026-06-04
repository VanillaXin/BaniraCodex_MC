package xin.vanilla.banira.client.event;

import net.minecraft.util.ResourceLocation;
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

    @Test
    public void drawContextUsesNoopClientFallbackSafely() {
        BaniraDrawContext draw = new BaniraDrawContext(null, 200, 100, 0.5F);

        assertEquals(18, draw.textWidth("abc"));
        assertEquals(9, draw.lineHeight());
        assertEquals(28, draw.drawText("abc", 10, 20, 0xFFFFFFFF, true));

        draw.fill(0, 0, 10, 10, 0xAA000000);
        draw.fillScreen(0x33000000);
        draw.blit(new ResourceLocation("banira_codex", "textures/gui/missing.png"), 0, 0, 0, 0, 8, 8, 8, 8);
        draw.drawTexture(new ResourceLocation("banira_codex", "textures/gui/missing.png"), 0, 0, 8, 8);
        draw.drawTexture(new ResourceLocation("banira_codex", "textures/gui/missing.png"), 0, 0, 8, 8, 16, 16);
        draw.horizontalLine(1, 2, 3, 0xFFFFFFFF);
        draw.verticalLine(1, 2, 3, 0xFFFFFFFF);
        draw.outline(0, 0, 10, 10, 1, 0xFFFFFFFF);
        draw.push();
        draw.translate(1, 2, 3);
        draw.scale(1.0F, 1.0F, 1.0F);
        draw.pop();
        draw.withTransform(() -> draw.translate(4, 5, 6));
    }
}
