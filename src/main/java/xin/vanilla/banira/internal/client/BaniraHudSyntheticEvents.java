package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.event.BaniraDrawContext;
import xin.vanilla.banira.client.event.BaniraHudOverlayElement;
import xin.vanilla.banira.client.event.BaniraHudRenderEvent;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * Focused synthetic HUD hooks for elements not exposed by a loader event on older branches.
 */
public final class BaniraHudSyntheticEvents {

    private BaniraHudSyntheticEvents() {
    }

    public static boolean beforeExperienceText(MatrixStack stack, int x) {
        BaniraDrawContext draw = new BaniraDrawContext(stack, screenWidth(), screenHeight(), 0.0F);
        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                BaniraHudOverlayElement.EXPERIENCE_TEXT,
                draw,
                true,
                BaniraHudGeometry.experienceTextBounds(x, draw.height())
        );
        BaniraClientEventHub.dispatchHudPreRender(event);
        if (event.canceled()) {
            BaniraClientEventHub.dispatchHudPostRender(new BaniraHudRenderEvent(
                    BaniraHudOverlayElement.EXPERIENCE_TEXT,
                    draw,
                    false,
                    BaniraHudGeometry.experienceTextBounds(x, draw.height())
            ));
        }
        return event.canceled();
    }

    public static void afterExperienceText(MatrixStack stack, int x) {
        BaniraDrawContext draw = new BaniraDrawContext(stack, screenWidth(), screenHeight(), 0.0F);
        BaniraClientEventHub.dispatchHudPostRender(new BaniraHudRenderEvent(
                BaniraHudOverlayElement.EXPERIENCE_TEXT,
                draw,
                false,
                BaniraHudGeometry.experienceTextBounds(x, draw.height())
        ));
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
