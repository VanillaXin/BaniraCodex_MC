package xin.vanilla.banira.client.event;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import xin.vanilla.banira.client.data.BaniraColorThemeLoader;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.NotificationManager;

/**
 * 客户端 Forge 游戏总线（{@code Dist.CLIENT}）：将事件转发至 {@link BaniraClientEventHub}，并处理本 Mod 的 GUI 逻辑（如 {@link QuickActionOverlay}）
 */
public final class BaniraClientForgeEventHandler {

    private BaniraClientForgeEventHandler() {
    }

    // region BaniraClientEventHub Forge 转发

    public static void register(IEventBus gameEventBus) {
        gameEventBus.addListener(ClientPlayerNetworkEvent.LoggingIn.class, BaniraClientForgeEventHandler::onClientPlayerLoggedIn);
        gameEventBus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, BaniraClientForgeEventHandler::onClientPlayerLoggedOut);
        gameEventBus.addListener(ClientTickEvent.Post.class, BaniraClientForgeEventHandler::onClientTick);
        gameEventBus.addListener(ClientChatEvent.class, BaniraClientForgeEventHandler::onClientChat);
        gameEventBus.addListener(AddReloadListenerEvent.class, BaniraClientForgeEventHandler::onAddReloadListener);
        gameEventBus.addListener(ScreenEvent.Opening.class, BaniraClientForgeEventHandler::onGuiOpen);
        gameEventBus.addListener(ScreenEvent.Init.Pre.class, BaniraClientForgeEventHandler::onInitScreenPre);
        gameEventBus.addListener(ScreenEvent.Init.Post.class, BaniraClientForgeEventHandler::onInitScreenPost);
        gameEventBus.addListener(ScreenEvent.Render.Pre.class, BaniraClientForgeEventHandler::onDrawScreenPre);
        gameEventBus.addListener(ScreenEvent.Render.Post.class, BaniraClientForgeEventHandler::onDrawScreenPost);
        gameEventBus.addListener(EventPriority.LOWEST, ScreenEvent.Render.Post.class, BaniraClientForgeEventHandler::onDrawScreenPostInventoryQuickAction);
        gameEventBus.addListener(RenderGuiEvent.Post.class, BaniraClientForgeEventHandler::onRenderGuiPost);
        gameEventBus.addListener(EventPriority.HIGHEST, ScreenEvent.MouseButtonPressed.Pre.class, BaniraClientForgeEventHandler::onGuiMouseClickedPre);
        gameEventBus.addListener(EventPriority.HIGHEST, ScreenEvent.MouseButtonReleased.Pre.class, BaniraClientForgeEventHandler::onGuiMouseReleasedPre);
        gameEventBus.addListener(EventPriority.HIGHEST, ScreenEvent.MouseScrolled.Pre.class, BaniraClientForgeEventHandler::onGuiMouseScrollPre);
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedIn(event);
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedOut(event);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        BaniraClientEventHub.dispatchClientTick(event);
        NotificationManager.get().tickOutOfScreenClick();
    }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        BaniraClientEventHub.dispatchClientChat(event);
    }

    // endregion BaniraClientEventHub Forge 转发

    // region 资源重载

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(BaniraColorThemeLoader.INSTANCE);
    }

    // endregion 资源重载

    // region 本 Mod GUI（快捷栏 overlay 等）

    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event) {
        QuickActionOverlay.get().resetInteractionState();
        BaniraClientEventHub.Client.fireGuiChanged(event);
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @SubscribeEvent
    public static void onInitScreenPre(ScreenEvent.Init.Pre event) {
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @SubscribeEvent
    public static void onInitScreenPost(ScreenEvent.Init.Post event) {
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @SubscribeEvent
    public static void onDrawScreenPre(ScreenEvent.Render.Pre event) {
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getScreen())) {
            QuickActionOverlay.get().tickInteraction(event.getScreen(), event.getMouseX(), event.getMouseY());
        }
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @SubscribeEvent
    public static void onDrawScreenPost(ScreenEvent.Render.Post event) {
        BaniraClientEventHub.Client.fireDrawScreenPost(event);
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawScreenPostInventoryQuickAction(ScreenEvent.Render.Post event) {
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getScreen())) {
            QuickActionOverlay.get().render(event.getGuiGraphics(), event.getScreen(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
        NotificationManager.get().render(event.getGuiGraphics());
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) {
            NotificationManager.get().render(event.getGuiGraphics());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        if (QuickActionOverlay.get().handleMouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (NotificationManager.get().tryHandleHudClick(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        if (QuickActionOverlay.get().handleMouseReleased(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(ScreenEvent.MouseScrolled.Pre event) {
        if (QuickActionOverlay.get().handleMouseScroll(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    // endregion 本 Mod GUI（快捷栏 overlay 等）

}
