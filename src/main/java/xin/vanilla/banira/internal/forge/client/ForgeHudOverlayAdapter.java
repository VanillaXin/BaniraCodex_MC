package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import xin.vanilla.banira.api.client.hud.*;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.BaniraClientEventHub;
import xin.vanilla.banira.internal.client.BaniraHudGeometry;

import javax.annotation.Nonnull;

/**
 * Forge 1.21.1 已移除逐浮层事件；由窄 Mixin 把原版 HUD 方法转换为 Banira 事件。
 */
public final class ForgeHudOverlayAdapter {

    private ForgeHudOverlayAdapter() {
    }

    public static boolean dispatchPre(@Nonnull HudOverlayElement element, @Nonnull GuiGraphics graphics) {
        float partialTick = partialTick();
        BaniraHudRenderContext context = context(graphics, partialTick);
        BaniraHudRenderEvent baniraEvent = new BaniraHudRenderEvent(
                HudRenderPhase.PRE,
                element,
                context,
                bounds(element, context.screenWidth(), context.screenHeight()),
                true
        );
        BaniraHudEvents.dispatchPre(baniraEvent);
        BaniraClientEventHub.dispatchRenderOverlayPreNative(element, graphics.pose(), partialTick,
                BaniraClientRuntime.currentScreen() != null);
        return baniraEvent.canceled();
    }

    public static void dispatchPost(@Nonnull HudOverlayElement element, @Nonnull GuiGraphics graphics) {
        float partialTick = partialTick();
        BaniraHudRenderContext context = context(graphics, partialTick);
        BaniraHudEvents.dispatchPost(new BaniraHudRenderEvent(
                HudRenderPhase.POST,
                element,
                context,
                bounds(element, context.screenWidth(), context.screenHeight()),
                false
        ));
        BaniraClientEventHub.Client.fireRenderOverlayPostNative(element, graphics.pose(), partialTick,
                BaniraClientRuntime.currentScreen() != null);
    }

    private static BaniraHudRenderContext context(@Nonnull GuiGraphics graphics, float partialTick) {
        KeyValue<Integer, Integer> screen = BaniraClientRuntime.guiScaledSize();
        return new BaniraHudRenderContext(
                new xin.vanilla.banira.api.client.render.BaniraDrawContext(
                        new ForgeBaniraDrawHandle(graphics.pose()), screen.key(), screen.val(), partialTick),
                screen.key(),
                screen.val(),
                partialTick
        );
    }

    private static float partialTick() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static HudOverlayElement mapElement(String overlayId) {
        if (overlayId == null) {
            return HudOverlayElement.UNKNOWN;
        }
        switch (overlayId.toUpperCase(java.util.Locale.ROOT)) {
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
