package xin.vanilla.banira.internal.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.client.event.*;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonManager;
import xin.vanilla.banira.client.gui.quickaction.ExternalInventoryButtonSmokeRunner;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.client.BaniraClientEventHub;
import xin.vanilla.banira.internal.client.BaniraClientOverlayBridge;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;

/**
 * 客户端 NeoForge 游戏总线（{@code Dist.CLIENT}）：只做加载器事件到 Banira 事件的转换。
 */
@EventBusSubscriber(modid = Banira.MOD_ID, value = Dist.CLIENT)
public final class BaniraClientNeoForgeEventHandler {

    private BaniraClientNeoForgeEventHandler() {
    }

    // region BaniraClientEventHub NeoForge 转发

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedIn(event.getPlayer());
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BaniraClientEventHub.dispatchClientPlayerLoggedOut(event.getPlayer());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ManagedConfigFiles.poll(ManagedConfigFiles.Scope.CLIENT);
        BaniraClientEventHub.dispatchClientTick(BaniraClientTickEvent.END);
        BaniraScheduler.dispatchClientTick();
        BaniraClientOverlayBridge.tickOutOfScreenNotifications();
        ExternalInventoryButtonSmokeRunner.onClientTick();
    }

    /** 没有打开界面时，按键仍需进入 Banira 的快捷入口分发链。 */
    @SubscribeEvent
    public static void onGlobalKeyInput(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) return;
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
            BaniraClientEventHub.dispatchKeyPressedPre(BaniraKeyboardEvent.pressed(
                    BaniraScreenInfo.closed(), event.getKey(), event.getScanCode(), event.getModifiers()));
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            BaniraClientEventHub.dispatchKeyReleasedPost(BaniraKeyboardEvent.released(
                    BaniraScreenInfo.closed(), event.getKey(), event.getScanCode(), event.getModifiers()));
        }
    }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        BaniraClientEventHub.dispatchClientChat(new BaniraChatEvent(event.getMessage()));
    }

    @SubscribeEvent
    public static void onGuiScreen(ScreenEvent.Render.Pre event) {
        BaniraClientEventHub.dispatchGuiScreen(new BaniraScreenEvent(BaniraClientEventHub.screenInfo(event.getScreen())));
    }

    // endregion BaniraClientEventHub NeoForge 转发

    // region 本 Mod GUI（快捷栏 overlay 等）

    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event) {
        BaniraClientEventHub.Client.fireGuiChanged(new BaniraScreenOpenEvent(BaniraClientEventHub.screenInfo(event.getScreen())));
        ExternalInventoryButtonManager.get().refreshForScreen(event.getScreen());
    }

    @SubscribeEvent
    public static void onDrawScreenPre(ScreenEvent.Render.Pre event) {
        BaniraClientEventHub.Client.fireDrawScreenPreNative(
                event.getGuiGraphics(),
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY(),
                event.getPartialTick()
        );
    }

    @SubscribeEvent
    public static void onDrawScreenPost(ScreenEvent.Render.Post event) {
        BaniraClientEventHub.Client.fireDrawScreenPostNative(
                event.getGuiGraphics(),
                event.getScreen(),
                event.getMouseX(),
                event.getMouseY(),
                event.getPartialTick()
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseClickedPre(ScreenEvent.MouseButtonPressed.Pre event) {
        BaniraMouseEvent mouseEvent = BaniraMouseEvent.clicked(BaniraClientEventHub.screenInfo(event.getScreen()), event.getMouseX(), event.getMouseY(), event.getButton());
        BaniraClientEventHub.dispatchMouseClickedPre(mouseEvent, event.getScreen());
        event.setCanceled(mouseEvent.canceled());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        BaniraMouseEvent mouseEvent = BaniraMouseEvent.released(BaniraClientEventHub.screenInfo(event.getScreen()), event.getMouseX(), event.getMouseY(), event.getButton());
        BaniraClientEventHub.dispatchMouseReleasedPre(mouseEvent, event.getScreen());
        event.setCanceled(mouseEvent.canceled());
    }

    @SubscribeEvent
    public static void onGuiMouseReleasedPost(ScreenEvent.MouseButtonReleased.Post event) {
        BaniraClientEventHub.dispatchMouseReleasedPost(BaniraMouseEvent.released(
                BaniraClientEventHub.screenInfo(event.getScreen()),
                event.getMouseX(),
                event.getMouseY(),
                event.getButton()
        ));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onGuiMouseScrollPre(ScreenEvent.MouseScrolled.Pre event) {
        double delta = event.getScrollDeltaY() != 0
                ? event.getScrollDeltaY()
                : event.getScrollDeltaX();
        BaniraMouseEvent mouseEvent = BaniraMouseEvent.scrolled(BaniraClientEventHub.screenInfo(event.getScreen()), event.getMouseX(), event.getMouseY(), delta);
        BaniraClientEventHub.dispatchMouseScrolledPre(mouseEvent, event.getScreen());
        event.setCanceled(mouseEvent.canceled());
    }

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

    @SubscribeEvent
    public static void onGuiKeyReleasedPost(ScreenEvent.KeyReleased.Post event) {
        BaniraClientEventHub.dispatchKeyReleasedPost(BaniraKeyboardEvent.released(
                BaniraClientEventHub.screenInfo(event.getScreen()),
                event.getKeyCode(),
                event.getScanCode(),
                event.getModifiers()
        ));
    }

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

    // endregion 本 Mod GUI（快捷栏 overlay 等）

}
