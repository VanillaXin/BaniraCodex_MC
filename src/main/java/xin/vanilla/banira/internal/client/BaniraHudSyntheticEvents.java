package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import xin.vanilla.banira.api.client.hud.*;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * Focused synthetic HUD hooks for elements not exposed by a loader event on older branches.
 */
public final class BaniraHudSyntheticEvents {

    private BaniraHudSyntheticEvents() {
    }

    public static boolean beforeExperienceBar(PoseStack stack, int x) {
        BaniraDrawContext draw = drawContext(stack);
        BaniraHudRenderEvent event = preEvent(
                HudOverlayElement.EXPERIENCE_BAR,
                draw,
                BaniraHudGeometry.experienceBarBounds(x, draw.screenHeight())
        );
        BaniraHudEvents.dispatchPre(event);
        return event.canceled();
    }

    public static void afterExperienceBar(PoseStack stack, int x) {
        BaniraDrawContext draw = drawContext(stack);
        dispatchPost(HudOverlayElement.EXPERIENCE_BAR, draw,
                BaniraHudGeometry.experienceBarBounds(x, draw.screenHeight()));
    }

    public static boolean beforeExperienceText(PoseStack stack, int x) {
        BaniraDrawContext draw = drawContext(stack);
        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                HudRenderPhase.PRE,
                HudOverlayElement.EXPERIENCE_TEXT,
                hudContext(draw),
                BaniraHudGeometry.experienceTextBounds(x, draw.screenHeight()),
                true
        );
        BaniraHudEvents.dispatchPre(event);
        if (event.canceled()) {
            BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                    HudRenderPhase.POST,
                    HudOverlayElement.EXPERIENCE_TEXT,
                    hudContext(draw),
                    BaniraHudGeometry.experienceTextBounds(x, draw.screenHeight()),
                    false
            ));
        }
        return event.canceled();
    }

    public static void afterExperienceText(PoseStack stack, int x) {
        BaniraDrawContext draw = drawContext(stack);
        BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                HudRenderPhase.POST,
                HudOverlayElement.EXPERIENCE_TEXT,
                hudContext(draw),
                BaniraHudGeometry.experienceTextBounds(x, draw.screenHeight()),
                false
        ));
    }

    private static BaniraHudRenderEvent preEvent(HudOverlayElement element, BaniraDrawContext draw,
                                                  BaniraHudBounds bounds) {
        return new BaniraHudRenderEvent(HudRenderPhase.PRE, element, hudContext(draw), bounds, true);
    }

    private static void dispatchPost(HudOverlayElement element, BaniraDrawContext draw, BaniraHudBounds bounds) {
        BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                HudRenderPhase.POST, element, hudContext(draw), bounds, false));
    }

    private static BaniraDrawContext drawContext(PoseStack stack) {
        return new BaniraDrawContext(new BaniraLegacyDrawHandle(stack), screenWidth(), screenHeight(), 0.0F);
    }

    private static BaniraHudRenderContext hudContext(BaniraDrawContext draw) {
        return new BaniraHudRenderContext(draw, draw.screenWidth(), draw.screenHeight(), draw.partialTick());
    }

    private static int screenWidth() {
        KeyValue<Integer, Integer> size = guiScaledSize();
        return size != null ? size.key() : 0;
    }

    private static int screenHeight() {
        KeyValue<Integer, Integer> size = guiScaledSize();
        return size != null ? size.val() : 0;
    }

    private static KeyValue<Integer, Integer> guiScaledSize() {
        return BaniraPlatforms.isInstalled() && BaniraPlatforms.get().isClient()
                ? BaniraClientAccess.guiScaledSize()
                : null;
    }
}
