package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import xin.vanilla.banira.api.client.hud.*;

import javax.annotation.Nonnull;

/**
 * Forge 1.18.2 HUD 事件转换；不同版本只替换这一层。
 */
public final class ForgeHudOverlayAdapter {

    private ForgeHudOverlayAdapter() {
    }

    public static void dispatchPre(@Nonnull RenderGameOverlayEvent.Pre event) {
        BaniraHudRenderEvent baniraEvent = new BaniraHudRenderEvent(
                HudRenderPhase.PRE,
                mapElement(event.getType()),
                context(event),
                event.isCancelable()
        );
        BaniraHudEvents.dispatchPre(baniraEvent);
        if (baniraEvent.canceled()) {
            event.setCanceled(true);
        }
    }

    public static void dispatchPost(@Nonnull RenderGameOverlayEvent.Post event) {
        BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                HudRenderPhase.POST,
                mapElement(event.getType()),
                context(event),
                false
        ));
    }

    private static BaniraHudRenderContext context(@Nonnull RenderGameOverlayEvent event) {
        Minecraft mc = Minecraft.getInstance();
        return new BaniraHudRenderContext(
                event.getMatrixStack(),
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                event.getPartialTicks()
        );
    }

    private static HudOverlayElement mapElement(RenderGameOverlayEvent.ElementType type) {
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
}
