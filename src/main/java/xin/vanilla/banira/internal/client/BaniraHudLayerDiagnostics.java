package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.event.BaniraDrawContext;
import xin.vanilla.banira.client.event.BaniraHudLayers;
import xin.vanilla.banira.client.event.BaniraHudOverlayElement;

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
        BaniraHudLayers.interceptExperienceBar(event -> drawInterceptMark(event.draw(), 0x8848D46A));
        BaniraHudLayers.interceptExperienceText(event -> drawInterceptMark(event.draw(), 0x8855FF77));
        BaniraHudLayers.after(BaniraHudOverlayElement.EXPERIENCE_BAR, event -> drawDiagnosticBar(event.draw()));
        BaniraHudLayers.after(BaniraHudOverlayElement.EXPERIENCE_TEXT, event -> drawDiagnosticText(event.draw()));
    }

    private static void drawInterceptMark(BaniraDrawContext draw, int color) {
        if (draw == null) {
            return;
        }
        draw.fill(draw.width() - 10, draw.height() - 10, 4, 4, color);
    }

    private static void drawDiagnosticBar(BaniraDrawContext draw) {
        if (draw == null) {
            return;
        }
        int width = 91;
        int x = (draw.width() - width) / 2;
        int y = draw.height() - 32;
        draw.fill(x, y, width, 5, 0xAA111111);
        draw.fill(x, y, width / 2, 5, 0xFF48D46A);
    }

    private static void drawDiagnosticText(BaniraDrawContext draw) {
        if (draw == null) {
            return;
        }
        draw.drawCenteredText("Banira HUD", draw.width() / 2, draw.height() - 39, 0xFF55FF77, true);
    }
}
