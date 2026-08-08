package xin.vanilla.banira.internal.forge.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.client.event.*;
import xin.vanilla.banira.api.client.hud.*;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.internal.client.*;

/**
 * Forge 1.16.5 客户端事件桥；只在这里接触 Forge 原生事件和 MatrixStack。
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeBaniraClientEventBridge {
    private ForgeBaniraClientEventBridge() {
    }

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
        if (event.phase == TickEvent.Phase.END) {
            BaniraClientEventHub.dispatchClientTick(BaniraClientTickEvent.END);
            BaniraClientGuiService.handleClientTickEnd(!BaniraClientAccess.hasScreen());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        BaniraClientEventHub.dispatchClientChat(new BaniraChatEvent(event.getMessage()));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiScreen(GuiScreenEvent event) {
        BaniraClientEventHub.dispatchGuiScreen(new BaniraScreenEvent(BaniraClientEventHub.screenInfo(event.getGui())));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        BaniraHudRenderEvent baniraEvent = hudEvent(event, HudRenderPhase.PRE, event.isCancelable());
        BaniraHudEvents.dispatchPre(baniraEvent);
        BaniraClientEventHub.dispatchRenderOverlayPreNative(baniraEvent.element(), event.getMatrixStack(), event.getPartialTicks(), BaniraClientAccess.hasScreen());
        if (baniraEvent.canceled() && event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        BaniraHudRenderEvent baniraEvent = hudEvent(event, HudRenderPhase.POST, false);
        BaniraHudEvents.dispatchPost(baniraEvent);
        BaniraClientEventHub.Client.fireRenderOverlayPostNative(baniraEvent.element(), event.getMatrixStack(), event.getPartialTicks(), BaniraClientAccess.hasScreen());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(BaniraClientResourceService.colorThemeLoader());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        BaniraClientEventHub.Client.fireGuiChanged(new BaniraScreenOpenEvent(BaniraClientEventHub.screenInfo(event.getGui())));
        ExternalInventoryButtonManager.get().refreshForScreen(event.getGui());
        BaniraClientGuiService.handleScreenOpened();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        BaniraClientEventHub.Client.fireTextureReload(new BaniraTextureReloadEvent(event.getMap().location().toString()));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        BaniraClientGuiService.handleScreenPreRender(event.getGui(), event.getMouseX(), event.getMouseY());
        BaniraClientEventHub.Client.fireDrawScreenPreNative(
                event.getMatrixStack(),
                event.getGui(),
                event.getMouseX(),
                event.getMouseY(),
                event.getRenderPartialTicks()
        );
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        BaniraClientEventHub.Client.fireDrawScreenPostNative(
                event.getMatrixStack(),
                event.getGui(),
                event.getMouseX(),
                event.getMouseY(),
                event.getRenderPartialTicks()
        );
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(GuiScreenEvent.MouseClickedEvent.Pre event) {
        BaniraMouseEvent baniraEvent = BaniraMouseEvent.clicked(screenInfo(event.getGui()), event.getMouseX(), event.getMouseY(), event.getButton());
        BaniraClientEventHub.dispatchMouseClickedPre(baniraEvent, event.getGui());
        if (baniraEvent.canceled() || BaniraClientGuiService.handleMouseClicked(event.getGui(), baniraEvent)) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(GuiScreenEvent.MouseReleasedEvent.Pre event) {
        BaniraMouseEvent baniraEvent = BaniraMouseEvent.released(screenInfo(event.getGui()), event.getMouseX(), event.getMouseY(), event.getButton());
        BaniraClientEventHub.dispatchMouseReleasedPre(baniraEvent, event.getGui());
        if (baniraEvent.canceled() || BaniraClientGuiService.handleMouseReleased(event.getGui(), baniraEvent)) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(GuiScreenEvent.MouseScrollEvent.Pre event) {
        BaniraMouseEvent baniraEvent = BaniraMouseEvent.scrolled(screenInfo(event.getGui()), event.getMouseX(), event.getMouseY(), event.getScrollDelta());
        BaniraClientEventHub.dispatchMouseScrolledPre(baniraEvent, event.getGui());
        if (baniraEvent.canceled() || BaniraClientGuiService.handleMouseScrolled(event.getGui(), baniraEvent)) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyPressed(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        BaniraKeyboardEvent baniraEvent = BaniraKeyboardEvent.pressed(screenInfo(event.getGui()), event.getKeyCode(), event.getScanCode(), event.getModifiers());
        BaniraClientEventHub.dispatchKeyPressedPre(baniraEvent);
        BaniraClientGuiService.handleKeyboard(baniraEvent);
        if (baniraEvent.canceled()) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyReleased(GuiScreenEvent.KeyboardKeyReleasedEvent.Post event) {
        BaniraKeyboardEvent baniraEvent = BaniraKeyboardEvent.released(screenInfo(event.getGui()), event.getKeyCode(), event.getScanCode(), event.getModifiers());
        BaniraClientEventHub.dispatchKeyReleasedPost(baniraEvent);
        BaniraClientGuiService.handleKeyboard(baniraEvent);
    }

    /** 游戏内没有打开界面时，Forge 不会触发 GuiScreenEvent。 */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGlobalKeyInput(InputEvent.KeyInputEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
            BaniraKeyboardEvent baniraEvent = BaniraKeyboardEvent.pressed(
                    BaniraScreenInfo.closed(), event.getKey(), event.getScanCode(), event.getModifiers());
            BaniraClientEventHub.dispatchKeyPressedPre(baniraEvent);
            BaniraClientGuiService.handleKeyboard(baniraEvent);
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            BaniraKeyboardEvent baniraEvent = BaniraKeyboardEvent.released(
                    BaniraScreenInfo.closed(), event.getKey(), event.getScanCode(), event.getModifiers());
            BaniraClientEventHub.dispatchKeyReleasedPost(baniraEvent);
            BaniraClientGuiService.handleKeyboard(baniraEvent);
        }
    }

    private static BaniraHudRenderEvent hudEvent(RenderGameOverlayEvent event, HudRenderPhase phase, boolean cancellable) {
        Minecraft minecraft = Minecraft.getInstance();
        HudOverlayElement element = toBaniraElement(event.getType());
        BaniraDrawContext draw = drawContext(
                event.getMatrixStack(),
                minecraft.getWindow().getGuiScaledWidth(),
                minecraft.getWindow().getGuiScaledHeight(),
                event.getPartialTicks()
        );
        return new BaniraHudRenderEvent(
                phase,
                element,
                new BaniraHudRenderContext(draw, draw.screenWidth(), draw.screenHeight(), draw.partialTick()),
                defaultHudBounds(element, draw),
                cancellable
        );
    }

    private static BaniraDrawContext drawContext(MatrixStack stack, int width, int height, float partialTicks) {
        return new BaniraDrawContext(new BaniraLegacyDrawHandle(stack), width, height, partialTicks);
    }

    private static BaniraHudBounds defaultHudBounds(HudOverlayElement element, BaniraDrawContext draw) {
        if (element == HudOverlayElement.EXPERIENCE_BAR || element == HudOverlayElement.EXPERIENCE) {
            return BaniraHudGeometry.experienceBarBounds((draw.screenWidth() - 182) / 2, draw.screenHeight());
        }
        if (element == HudOverlayElement.EXPERIENCE_TEXT) {
            return BaniraHudGeometry.experienceTextBounds((draw.screenWidth() - 182) / 2, draw.screenHeight());
        }
        return BaniraHudBounds.empty();
    }

    private static xin.vanilla.banira.api.client.event.BaniraScreenInfo screenInfo(Screen screen) {
        return BaniraClientEventHub.screenInfo(screen);
    }

    private static HudOverlayElement toBaniraElement(RenderGameOverlayEvent.ElementType type) {
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
            case "JUMPBAR":
                return HudOverlayElement.EXPERIENCE_BAR;
            case "EXPERIENCE_LEVEL":
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
            case "BOSSINFO":
            case "BOSS_EVENT_PROGRESS":
                return HudOverlayElement.BOSS_HEALTH;
            case "PLAYER_LIST":
            case "PLAYERLIST":
                return HudOverlayElement.PLAYER_LIST;
            case "DEBUG":
            case "FPS_GRAPH":
                return HudOverlayElement.DEBUG_TEXT;
            case "TEXT":
                return HudOverlayElement.TEXT;
            default:
                return HudOverlayElement.UNKNOWN;
        }
    }
}
