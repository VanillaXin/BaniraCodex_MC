package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.matrix.MatrixStack;
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

    public static boolean beforeExperienceText(MatrixStack stack, int x) {
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

    public static void afterExperienceText(MatrixStack stack, int x) {
        BaniraDrawContext draw = drawContext(stack);
        BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                HudRenderPhase.POST,
                HudOverlayElement.EXPERIENCE_TEXT,
                hudContext(draw),
                BaniraHudGeometry.experienceTextBounds(x, draw.screenHeight()),
                false
        ));
    }

    private static BaniraDrawContext drawContext(MatrixStack stack) {
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
