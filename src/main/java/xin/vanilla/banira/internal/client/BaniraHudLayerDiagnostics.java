package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.client.event.BaniraDrawContext;
import xin.vanilla.banira.client.event.BaniraHudLayers;

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
        BaniraHudLayers.replaceExperienceBar(event -> drawDiagnosticBar(event.draw()));
        BaniraHudLayers.replaceExperienceText(event -> drawDiagnosticText(event.draw()));
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
