package xin.vanilla.banira.internal.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.client.event.*;
import xin.vanilla.banira.client.data.BaniraColorThemeLoader;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.client.BaniraClientEventHub;
import xin.vanilla.banira.internal.client.BaniraClientOverlayBridge;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;

/**
 * 客户端 Forge 游戏总线（{@code Dist.CLIENT}）：只做 Forge 事件到 Banira 事件的转换。
 */
@Mod.EventBusSubscriber(modid = Banira.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BaniraClientForgeEventHandler {

    private BaniraClientForgeEventHandler() {
    }

    // region BaniraClientEventHub Forge 转发

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedIn(event.getPlayer());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedOut(event.getPlayer());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BaniraClientEventHub.dispatchClientTick(BaniraClientTickEvent.END);
            BaniraScheduler.dispatchClientTick();
            BaniraClientOverlayBridge.tickOutOfScreenNotifications();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        BaniraClientEventHub.dispatchClientChat(new BaniraChatEvent(event.getMessage()));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiScreen(ScreenEvent event) {
        BaniraClientEventHub.dispatchGuiScreen(new BaniraScreenEvent(BaniraClientEventHub.screenInfo(event.getScreen())));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        ForgeHudOverlayAdapter.dispatchPre(event);
        BaniraClientEventHub.dispatchRenderOverlayPreNative(
                ForgeHudOverlayAdapter.mapElement(event.getOverlay().id().getPath()),
                event.getPoseStack(),
                event.getPartialTick(),
                BaniraClientRuntime.currentScreen() != null
        );
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
        BaniraClientEventHub.Client.fireGuiChanged(new BaniraScreenOpenEvent(BaniraClientEventHub.screenInfo(event.getScreen())));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        BaniraClientEventHub.Client.fireTextureReload(new BaniraTextureReloadEvent(event.getAtlas().location().toString()));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPre(ScreenEvent.Render.Pre event) {
        BaniraClientEventHub.Client.fireDrawScreenPreNative(
                event.getPoseStack(),
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY(),
                event.getPartialTick()
        );
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onDrawScreenPost(ScreenEvent.Render.Post event) {
        BaniraClientEventHub.Client.fireDrawScreenPostNative(
                event.getPoseStack(),
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY(),
                event.getPartialTick()
        );
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        BaniraMouseEvent mouseEvent = BaniraMouseEvent.clicked(BaniraClientEventHub.screenInfo(event.getScreen()), event.getMouseX(), event.getMouseY(), event.getButton());
        BaniraClientEventHub.dispatchMouseClickedPre(mouseEvent, event.getScreen());
        event.setCanceled(mouseEvent.canceled());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        BaniraMouseEvent mouseEvent = BaniraMouseEvent.released(BaniraClientEventHub.screenInfo(event.getScreen()), event.getMouseX(), event.getMouseY(), event.getButton());
        BaniraClientEventHub.dispatchMouseReleasedPre(mouseEvent, event.getScreen());
        event.setCanceled(mouseEvent.canceled());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiMouseReleasedPost(ScreenEvent.MouseButtonReleased.Post event) {
        BaniraClientEventHub.dispatchMouseReleasedPost(BaniraMouseEvent.released(
                BaniraClientEventHub.screenInfo(event.getScreen()),
                event.getMouseX(),
                event.getMouseY(),
                event.getButton()
        ));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(ScreenEvent.MouseScrolled.Pre event) {
        BaniraMouseEvent mouseEvent = BaniraMouseEvent.scrolled(BaniraClientEventHub.screenInfo(event.getScreen()), event.getMouseX(), event.getMouseY(), event.getScrollDelta());
        BaniraClientEventHub.dispatchMouseScrolledPre(mouseEvent, event.getScreen());
        event.setCanceled(mouseEvent.canceled());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseDraggedPre(ScreenEvent.MouseDragged.Pre event) {
        BaniraMouseEvent mouseEvent = BaniraMouseEvent.dragged(
                BaniraClientEventHub.screenInfo(event.getScreen()),
                event.getMouseX(),
                event.getMouseY(),
                event.getMouseButton(),
                event.getDragX(),
                event.getDragY()
        );
        BaniraClientEventHub.dispatchMouseDraggedPre(mouseEvent, event.getScreen());
        event.setCanceled(mouseEvent.canceled());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiKeyPressedPre(ScreenEvent.KeyPressed.Pre event) {
        BaniraKeyboardEvent keyboardEvent = BaniraKeyboardEvent.pressed(
                BaniraClientEventHub.screenInfo(event.getScreen()),
                event.getKeyCode(),
                event.getScanCode(),
                event.getModifiers()
        );
        BaniraClientEventHub.dispatchKeyPressedPre(keyboardEvent);
        event.setCanceled(keyboardEvent.canceled());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiKeyReleasedPost(ScreenEvent.KeyReleased.Post event) {
        BaniraClientEventHub.dispatchKeyReleasedPost(BaniraKeyboardEvent.released(
                BaniraClientEventHub.screenInfo(event.getScreen()),
                event.getKeyCode(),
                event.getScanCode(),
                event.getModifiers()
        ));
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiCharTypedPre(ScreenEvent.CharacterTyped.Pre event) {
        BaniraKeyboardEvent keyboardEvent = BaniraKeyboardEvent.charTyped(
                BaniraClientEventHub.screenInfo(event.getScreen()),
                event.getCodePoint(),
                event.getModifiers()
        );
        BaniraClientEventHub.dispatchCharTypedPre(keyboardEvent);
        event.setCanceled(keyboardEvent.canceled());
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderOverlayPost(RenderGuiOverlayEvent.Post event) {
        ForgeHudOverlayAdapter.dispatchPost(event);
        BaniraClientEventHub.Client.fireRenderOverlayPostNative(
                ForgeHudOverlayAdapter.mapElement(event.getOverlay().id().getPath()),
                event.getPoseStack(),
                event.getPartialTick(),
                BaniraClientRuntime.currentScreen() != null
        );
    }

    // endregion 本 Mod GUI（快捷栏 overlay 等）

}
