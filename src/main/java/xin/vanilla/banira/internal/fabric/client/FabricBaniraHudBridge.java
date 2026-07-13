package xin.vanilla.banira.internal.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import xin.vanilla.banira.api.client.hud.BaniraHudBounds;
import xin.vanilla.banira.api.client.hud.BaniraHudEvents;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderContext;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderEvent;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.api.client.hud.HudRenderPhase;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.client.BaniraClientDrawBridge;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.BaniraHudGeometry;

import javax.annotation.Nonnull;

/** Fabric 原版 HUD 绘制点到稳定 Banira HUD 事件的转换层。 */
public final class FabricBaniraHudBridge {
    private FabricBaniraHudBridge() {
    }

    public static boolean dispatchPre(@Nonnull HudOverlayElement element, @Nonnull PoseStack stack) {
        BaniraHudRenderContext context = context(stack);
        BaniraHudRenderEvent event = new BaniraHudRenderEvent(
                HudRenderPhase.PRE, element, context, bounds(element, context), true);
        BaniraHudEvents.dispatchPre(event);
        return event.canceled();
    }

    public static void dispatchPost(@Nonnull HudOverlayElement element, @Nonnull PoseStack stack) {
        BaniraHudRenderContext context = context(stack);
        BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                HudRenderPhase.POST, element, context, bounds(element, context), false));
    }

    private static BaniraHudRenderContext context(PoseStack stack) {
        KeyValue<Integer, Integer> screen = BaniraClientRuntime.guiScaledSize();
        float partialTick = Minecraft.getInstance().getFrameTime();
        BaniraDrawContext draw = new BaniraDrawContext(
                BaniraClientDrawBridge.handle(stack), screen.key(), screen.val(), partialTick);
        return new BaniraHudRenderContext(draw, screen.key(), screen.val(), partialTick);
    }

    private static BaniraHudBounds bounds(HudOverlayElement element, BaniraHudRenderContext context) {
        int left = context.screenWidth() / 2 - 91;
        if (element == HudOverlayElement.EXPERIENCE_BAR) {
            return BaniraHudGeometry.experienceBarBounds(left, context.screenHeight());
        }
        if (element == HudOverlayElement.EXPERIENCE_TEXT) {
            return BaniraHudGeometry.experienceTextBounds(left, context.screenHeight());
        }
        return BaniraHudBounds.empty();
    }
}
