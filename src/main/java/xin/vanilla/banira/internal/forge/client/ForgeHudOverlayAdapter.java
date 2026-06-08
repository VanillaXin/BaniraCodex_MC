package xin.vanilla.banira.internal.forge.client;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import xin.vanilla.banira.api.client.hud.*;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.BaniraHudGeometry;

import javax.annotation.Nonnull;

/**
 * Forge 1.18.2 HUD 事件转换；不同版本只替换这一层。
 */
public final class ForgeHudOverlayAdapter {

    private ForgeHudOverlayAdapter() {
    }

    public static void dispatchPre(@Nonnull RenderGameOverlayEvent.Pre event) {
        BaniraHudRenderContext context = context(event);
        HudOverlayElement element = mapElement(event.getType());
        BaniraHudRenderEvent baniraEvent = new BaniraHudRenderEvent(
                HudRenderPhase.PRE,
                element,
                context,
                bounds(element, context.screenWidth(), context.screenHeight()),
                event.isCancelable()
        );
        BaniraHudEvents.dispatchPre(baniraEvent);
        if (baniraEvent.canceled()) {
            event.setCanceled(true);
        }
    }

    public static void dispatchPost(@Nonnull RenderGameOverlayEvent.Post event) {
        BaniraHudRenderContext context = context(event);
        HudOverlayElement element = mapElement(event.getType());
        BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                HudRenderPhase.POST,
                element,
                context,
                bounds(element, context.screenWidth(), context.screenHeight()),
                false
        ));
    }

    private static BaniraHudRenderContext context(@Nonnull RenderGameOverlayEvent event) {
        KeyValue<Integer, Integer> screen = BaniraClientRuntime.guiScaledSize();
        return new BaniraHudRenderContext(
                event.getMatrixStack(),
                screen.key(),
                screen.val(),
                event.getPartialTicks()
        );
    }

    public static HudOverlayElement mapElement(RenderGameOverlayEvent.ElementType type) {
        if (type == null) {
            return HudOverlayElement.UNKNOWN;
        }
        switch (type.name()) {
            case "ALL":
                return HudOverlayElement.ALL;
            case "HOTBAR":
                return HudOverlayElement.HOTBAR;
            case "EXPERIENCE":
            case "EXPERIENCE_BAR":
                return HudOverlayElement.EXPERIENCE;
            case "EXPERIENCE_TEXT":
                return HudOverlayElement.EXPERIENCE_TEXT;
            case "HEALTH":
            case "HEALTHMOUNT":
                return HudOverlayElement.HEALTH;
            case "ARMOR":
                return HudOverlayElement.ARMOR;
            case "FOOD":
                return HudOverlayElement.FOOD;
            case "AIR":
                return HudOverlayElement.AIR;
            case "CHAT":
                return HudOverlayElement.CHAT;
            case "CROSSHAIRS":
            case "CROSSHAIR":
                return HudOverlayElement.CROSSHAIR;
            case "BOSSHEALTH":
            case "BOSS_HEALTH":
                return HudOverlayElement.BOSS_HEALTH;
            case "PLAYER_LIST":
                return HudOverlayElement.PLAYER_LIST;
            case "DEBUG":
                return HudOverlayElement.DEBUG_TEXT;
            case "TEXT":
                return HudOverlayElement.TEXT;
            default:
                return HudOverlayElement.UNKNOWN;
        }
    }

    private static BaniraHudBounds bounds(HudOverlayElement element, int screenWidth, int screenHeight) {
        int left = screenWidth / 2 - 91;
        switch (element) {
            case EXPERIENCE:
            case EXPERIENCE_BAR:
                return BaniraHudGeometry.experienceBarBounds(left, screenHeight);
            case EXPERIENCE_TEXT:
                return BaniraHudGeometry.experienceTextBounds(left, screenHeight);
            default:
                return BaniraHudBounds.empty();
        }
    }
}
