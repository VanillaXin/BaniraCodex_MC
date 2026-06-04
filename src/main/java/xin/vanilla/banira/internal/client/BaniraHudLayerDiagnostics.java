package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.event.*;

/**
 * Disabled-by-default HUD smoke diagnostics for loader/version adapter work.
 */
public final class BaniraHudLayerDiagnostics {
    private static final String ENABLED_PROPERTY = "banira.debugHudLayers";
    private static volatile boolean registered;

    private BaniraHudLayerDiagnostics() {
    }

    public static void register() {
        if (registered || !Boolean.getBoolean(ENABLED_PROPERTY)) {
            return;
        }
        registered = true;
        // Keep the smoke hook interception-oriented; child mods usually only need to observe/cancel these layers.
        BaniraHudLayers.interceptExperienceBar(event -> drawInterceptMark(event, 0x8848D46A));
        BaniraHudLayers.interceptExperienceText(event -> drawInterceptMark(event, 0x8855FF77));
        BaniraHudLayers.after(BaniraHudOverlayElement.EXPERIENCE_BAR, BaniraHudLayerDiagnostics::drawDiagnosticBar);
        BaniraHudLayers.after(BaniraHudOverlayElement.EXPERIENCE_TEXT, BaniraHudLayerDiagnostics::drawDiagnosticText);
    }

    private static void drawInterceptMark(BaniraHudRenderEvent event, int color) {
        BaniraDrawContext draw = event.draw();
        if (draw == null) {
            return;
        }
        BaniraHudBounds bounds = event.bounds().isKnown()
                ? BaniraHudBounds.of(event.bounds().right() - 4, event.bounds().y() - 2, 4, 4)
                : BaniraHudBounds.of(draw.width() - 10, draw.height() - 10, 4, 4);
        draw.fill(bounds, color);
    }

    private static void drawDiagnosticBar(BaniraHudRenderEvent event) {
        BaniraDrawContext draw = event.draw();
        if (draw == null) {
            return;
        }
        BaniraHudBounds bounds = event.bounds().isKnown()
                ? event.bounds().inflate(1)
                : BaniraHudBounds.of((draw.width() - 91) / 2, draw.height() - 32, 91, 5);
        draw.fillHorizontalProgress(bounds, 0.5F, 0xAA111111, 0xFF48D46A);
    }

    private static void drawDiagnosticText(BaniraHudRenderEvent event) {
        BaniraDrawContext draw = event.draw();
        if (draw == null) {
            return;
        }
        BaniraHudBounds bounds = event.bounds().isKnown()
                ? event.bounds()
                : BaniraHudBounds.of(0, draw.height() - 39, draw.width(), 9);
        draw.drawCenteredText("Banira HUD", bounds.centerX(), bounds.y(), 0xFF55FF77, true);
    }
}
