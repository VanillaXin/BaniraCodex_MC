package xin.vanilla.banira.client.event;

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
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;

/**
 * 客户端 Forge 游戏总线（{@code Dist.CLIENT}）：将事件转发至 {@link BaniraClientEventHub}，并处理本 Mod 的 GUI 逻辑（如 {@link QuickActionOverlay}）
 */
@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BaniraClientForgeEventHandler {

    private BaniraClientForgeEventHandler() {
    }

    // region BaniraClientEventHub Forge 转发

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedIn(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedOut(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        BaniraClientEventHub.dispatchClientTick(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        BaniraClientEventHub.dispatchClientChat(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiScreen(ScreenEvent event) {
        BaniraClientEventHub.dispatchGuiScreen(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        BaniraClientEventHub.dispatchRenderOverlayPre(event);
    }

    // endregion BaniraClientEventHub Forge 转发

    // region 资源重载

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(BaniraColorThemeLoader.INSTANCE);
    }

    // endregion 资源重载

    // region 本 Mod GUI（快捷栏 overlay 等）

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event) {
        QuickActionOverlay.get().resetInteractionState();
        BaniraClientEventHub.Client.fireGuiChanged(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        BaniraClientEventHub.Client.fireTextureReload(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPre(ScreenEvent.Render.Pre event) {
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getScreen())) {
            QuickActionOverlay.get().tickInteraction(event.getScreen(), event.getMouseX(), event.getMouseY());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPost(ScreenEvent.Render.Post event) {
        BaniraClientEventHub.Client.fireDrawScreenPost(event);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawScreenPostInventoryQuickAction(ScreenEvent.Render.Post event) {
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getScreen())) {
            QuickActionOverlay.get().render(event.getPoseStack(), event.getScreen(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        if (QuickActionOverlay.get().handleMouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        if (QuickActionOverlay.get().handleMouseReleased(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(ScreenEvent.MouseScrolled.Pre event) {
        if (QuickActionOverlay.get().handleMouseScroll(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        BaniraClientEventHub.Client.fireRenderOverlayPost(event);
    }

    // endregion 本 Mod GUI（快捷栏 overlay 等）

}
