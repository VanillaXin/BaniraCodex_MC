package xin.vanilla.banira.internal.client;

import xin.vanilla.banira.api.client.hud.BaniraHudBounds;
import xin.vanilla.banira.api.client.hud.BaniraHudEvents;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderEvent;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;

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
        // 只在调试开关开启时注册，避免默认影响子 mod 的 HUD 拦截链。
        BaniraHudEvents.onElementPreRender(HudOverlayElement.EXPERIENCE_BAR, event -> drawInterceptMark(event, 0x8848D46A));
        BaniraHudEvents.onElementPreRender(HudOverlayElement.EXPERIENCE_TEXT, event -> drawInterceptMark(event, 0x8855FF77));
        BaniraHudEvents.onElementPostRender(HudOverlayElement.EXPERIENCE_BAR, BaniraHudLayerDiagnostics::drawDiagnosticBar);
        BaniraHudEvents.onElementPostRender(HudOverlayElement.EXPERIENCE_TEXT, BaniraHudLayerDiagnostics::drawDiagnosticText);
    }

    private static void drawInterceptMark(BaniraHudRenderEvent event, int color) {
        BaniraDrawContext draw = event.context().draw();
        BaniraHudBounds bounds = event.bounds().isKnown()
                ? BaniraHudBounds.of(event.bounds().right() - 4, event.bounds().y() - 2, 4, 4)
                : BaniraHudBounds.of(draw.screenWidth() - 10, draw.screenHeight() - 10, 4, 4);
        draw.fill(bounds, color);
    }

    private static void drawDiagnosticBar(BaniraHudRenderEvent event) {
        BaniraDrawContext draw = event.context().draw();
        BaniraHudBounds bounds = event.bounds().isKnown()
                ? event.bounds().inflate(1)
                : BaniraHudBounds.of((draw.screenWidth() - 91) / 2, draw.screenHeight() - 32, 91, 5);
        draw.progressBar(bounds, 0.5F, 0xAA111111, 0xFF48D46A);
    }

    private static void drawDiagnosticText(BaniraHudRenderEvent event) {
        BaniraDrawContext draw = event.context().draw();
        BaniraHudBounds bounds = event.bounds().isKnown()
                ? event.bounds()
                : BaniraHudBounds.of(0, draw.screenHeight() - 39, draw.screenWidth(), 9);
        draw.text("Banira HUD", Math.max(0, bounds.centerX() - 30), bounds.y(), 0xFF55FF77, true);
    }
}
