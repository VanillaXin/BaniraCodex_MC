package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.data.BaniraColorThemeLoader;
import xin.vanilla.banira.client.event.*;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.client.util.NotificationManager;

import java.util.Objects;

/**
 * Forge-side client bridge. Loader events are translated here before they reach
 * the stable Banira client API.
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeBaniraClientEventBridge {

    private ForgeBaniraClientEventBridge() {
    }

    // region BaniraClientEventHub Forge bridge

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedIn(event.getPlayer());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedOut(event.getPlayer());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        BaniraClientEventHub.dispatchClientTick(new BaniraClientTickEvent(toBaniraTickPhase(event.phase)));
        if (event.phase == TickEvent.Phase.END) {
            NotificationManager.get().tickOutOfScreenClick();
            InputStateManager.handleClientTickEnd(Minecraft.getInstance().screen == null);
            NotificationLogScreen.openHotkeyScreenIfPressed();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        BaniraClientChatEvent baniraEvent = new BaniraClientChatEvent(event.getMessage());
        BaniraClientEventHub.dispatchClientChat(baniraEvent);
        if (baniraEvent.canceled()) {
            event.setCanceled(true);
        } else if (!Objects.equals(baniraEvent.message(), event.getMessage())) {
            event.setMessage(baniraEvent.message() == null ? "" : baniraEvent.message());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiScreen(GuiScreenEvent event) {
        BaniraClientEventHub.dispatchClientScreen(toBaniraScreenEvent(BaniraClientScreenEventType.ANY, event.getGui(), null, 0, 0, 0));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        BaniraHudRenderEvent baniraEvent = toBaniraHudEvent(event, true);
        BaniraClientEventHub.dispatchHudPreRender(baniraEvent);
        if (baniraEvent.canceled() && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        BaniraClientEventHub.dispatchHudPostRender(toBaniraHudEvent(event, false));
    }

    // endregion BaniraClientEventHub Forge bridge

    // region Resource reload

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(BaniraColorThemeLoader.INSTANCE);
    }

    // endregion Resource reload

    // region Banira-owned GUI behavior

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        QuickActionOverlay.get().resetInteractionState();
        BaniraClientEventHub.dispatchClientScreenChanged(toBaniraScreenEvent(BaniraClientScreenEventType.OPEN, event.getGui(), null, 0, 0, 0));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        BaniraClientEventHub.dispatchClientTextureReload(new BaniraTextureReloadEvent(event.getMap().location()));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        InputStateManager.handleDrawScreenPre(event.getMouseX(), event.getMouseY());
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getGui())) {
            QuickActionOverlay.get().tickInteraction(event.getGui(), event.getMouseX(), event.getMouseY());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        BaniraClientEventHub.dispatchClientScreenPostRender(toBaniraScreenEvent(
                BaniraClientScreenEventType.DRAW_POST,
                event.getGui(),
                event.getMatrixStack(),
                event.getMouseX(),
                event.getMouseY(),
                event.getRenderPartialTicks()
        ));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawScreenPostInventoryQuickAction(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getGui())) {
            QuickActionOverlay.get().render(event.getMatrixStack(), event.getGui(), event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(GuiScreenEvent.MouseClickedEvent.Pre event) {
        InputStateManager.handleMouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
        if (QuickActionOverlay.get().handleMouseClicked(event.getGui(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (NotificationManager.get().tryHandleHudClick(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(GuiScreenEvent.MouseReleasedEvent.Pre event) {
        InputStateManager.handleMouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
        if (QuickActionOverlay.get().handleMouseReleased(event.getGui(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(GuiScreenEvent.MouseScrollEvent.Pre event) {
        InputStateManager.handleMouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDelta());
        if (QuickActionOverlay.get().handleMouseScroll(event.getGui(), event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyPressed(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        InputStateManager.handleKeyPressed(event.getKeyCode());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyReleased(GuiScreenEvent.KeyboardKeyReleasedEvent.Post event) {
        InputStateManager.handleKeyReleased(event.getKeyCode());
    }

    private static BaniraHudRenderEvent toBaniraHudEvent(RenderGameOverlayEvent event, boolean beforeVanilla) {
        Minecraft minecraft = Minecraft.getInstance();
        return new BaniraHudRenderEvent(
                toBaniraElement(event.getType()),
                new BaniraDrawContext(
                        event.getMatrixStack(),
                        minecraft.getWindow().getGuiScaledWidth(),
                        minecraft.getWindow().getGuiScaledHeight(),
                        event.getPartialTicks()
                ),
                beforeVanilla
        );
    }

    private static BaniraClientScreenEvent toBaniraScreenEvent(BaniraClientScreenEventType type, Object screen, Object nativeDrawContext,
                                                               double mouseX, double mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        BaniraDrawContext draw = nativeDrawContext == null ? null : new BaniraDrawContext(
                nativeDrawContext,
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                partialTicks
        );
        return new BaniraClientScreenEvent(type, screen, draw, mouseX, mouseY, partialTicks);
    }

    private static BaniraTickPhase toBaniraTickPhase(TickEvent.Phase phase) {
        return phase == TickEvent.Phase.START ? BaniraTickPhase.START : BaniraTickPhase.END;
    }

    private static BaniraHudOverlayElement toBaniraElement(RenderGameOverlayEvent.ElementType type) {
        if (type == null) {
            return BaniraHudOverlayElement.UNKNOWN;
        }
        // Name-based mapping keeps this adapter resilient to Forge enum churn across MC versions.
        switch (type.name()) {
            case "ALL":
                return BaniraHudOverlayElement.ALL;
            case "HELMET":
                return BaniraHudOverlayElement.HELMET;
            case "PORTAL":
                return BaniraHudOverlayElement.PORTAL;
            case "CROSSHAIRS":
            case "CROSSHAIR":
                return BaniraHudOverlayElement.CROSSHAIR;
            case "BOSSHEALTH":
            case "BOSSINFO":
            case "BOSS_EVENT_PROGRESS":
                return BaniraHudOverlayElement.BOSS_HEALTH;
            case "ARMOR":
                return BaniraHudOverlayElement.ARMOR;
            case "HEALTH":
            case "HEALTHMOUNT":
                return BaniraHudOverlayElement.HEALTH;
            case "FOOD":
                return BaniraHudOverlayElement.FOOD;
            case "AIR":
                return BaniraHudOverlayElement.AIR;
            case "HOTBAR":
                return BaniraHudOverlayElement.HOTBAR;
            case "EXPERIENCE":
            case "EXPERIENCE_BAR":
            case "JUMPBAR":
                return BaniraHudOverlayElement.EXPERIENCE_BAR;
            case "EXPERIENCE_LEVEL":
            case "EXPERIENCE_TEXT":
                return BaniraHudOverlayElement.EXPERIENCE_TEXT;
            case "CHAT":
                return BaniraHudOverlayElement.CHAT;
            case "PLAYER_LIST":
            case "PLAYERLIST":
                return BaniraHudOverlayElement.PLAYER_LIST;
            case "DEBUG":
            case "FPS_GRAPH":
                return BaniraHudOverlayElement.DEBUG;
            case "POTION_ICONS":
            case "EFFECT_ICONS":
                return BaniraHudOverlayElement.EFFECT_ICONS;
            case "SUBTITLES":
                return BaniraHudOverlayElement.SUBTITLES;
            case "VIGNETTE":
                return BaniraHudOverlayElement.VIGNETTE;
            default:
                return BaniraHudOverlayElement.UNKNOWN;
        }
    }

    // endregion Banira-owned GUI behavior

}
