package xin.vanilla.banira.client.event;

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
import xin.vanilla.banira.api.client.event.*;
import xin.vanilla.banira.client.data.BaniraColorThemeLoader;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.internal.forge.client.ForgeHudOverlayAdapter;

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
            NotificationManager.get().tickOutOfScreenClick();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        BaniraClientEventHub.dispatchClientChat(new BaniraChatEvent(event.getMessage(), event));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiScreen(ScreenEvent event) {
        BaniraClientEventHub.dispatchGuiScreen(new BaniraScreenEvent(event.getScreen(), event));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        ForgeHudOverlayAdapter.dispatchPre(event);
        BaniraClientEventHub.dispatchRenderOverlayPre(new BaniraOverlayRenderEvent(
                ForgeHudOverlayAdapter.mapElement(event.getType()),
                event.getMatrixStack(),
                event.getPartialTicks(),
                Minecraft.getInstance().screen != null,
                event
        ));
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
    public static void onGuiOpen(ScreenOpenEvent event) {
        QuickActionOverlay.get().resetInteractionState();
        BaniraClientEventHub.Client.fireGuiChanged(new BaniraScreenOpenEvent(event.getScreen(), event));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        BaniraClientEventHub.Client.fireTextureReload(new BaniraTextureReloadEvent(event.getAtlas().location(), event));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPre(ScreenEvent.DrawScreenEvent.Pre event) {
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getScreen())) {
            QuickActionOverlay.get().tickInteraction(event.getScreen(), event.getMouseX(), event.getMouseY());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPost(ScreenEvent.DrawScreenEvent.Post event) {
        BaniraClientEventHub.Client.fireDrawScreenPost(new BaniraDrawScreenEvent(
                event.getPoseStack(),
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY(),
                event.getPartialTicks(),
                event
        ));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawScreenPostInventoryQuickAction(ScreenEvent.DrawScreenEvent.Post event) {
        if (QuickActionOverlay.isSupportedInventoryScreen(event.getScreen())) {
            QuickActionOverlay.get().render(event.getPoseStack(), event.getScreen(), event.getMouseX(), event.getMouseY(), event.getPartialTicks());
            QuickActionOverlay.get().flushSaveIfNeeded();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(ScreenEvent.MouseClickedEvent.Pre event) {
        if (QuickActionOverlay.get().handleMouseClicked(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
            return;
        }
        if (NotificationManager.get().tryHandleHudClick(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(ScreenEvent.MouseReleasedEvent.Pre event) {
        if (QuickActionOverlay.get().handleMouseReleased(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(ScreenEvent.MouseScrollEvent.Pre event) {
        if (QuickActionOverlay.get().handleMouseScroll(event.getScreen(), event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        ForgeHudOverlayAdapter.dispatchPost(event);
        BaniraClientEventHub.Client.fireRenderOverlayPost(new BaniraOverlayRenderEvent(
                ForgeHudOverlayAdapter.mapElement(event.getType()),
                event.getMatrixStack(),
                event.getPartialTicks(),
                Minecraft.getInstance().screen != null,
                event
        ));
    }

    // endregion 本 Mod GUI（快捷栏 overlay 等）

}
