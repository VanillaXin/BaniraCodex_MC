package xin.vanilla.banira.client.event;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.client.event.*;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderContext;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.api.client.input.BaniraDragTracker;
import xin.vanilla.banira.api.client.input.BaniraKeyPressTracker;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.InputStateManager;
import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.client.util.TextureUtils;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.ModLoadedToBoth;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.client.BaniraClientRuntime;
import xin.vanilla.banira.internal.client.PlatformBaniraDrawHandle;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端专用事件回调与转发
 */
public final class BaniraClientEventHub {

    private BaniraClientEventHub() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedOutCallbacks = new ArrayList<>();

    private static final List<Consumer<BaniraScreenOpenEvent>> clientGuiChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraTextureReloadEvent>> clientTextureReloadCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraDrawScreenEvent>> clientDrawScreenPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraDrawScreenEvent>> clientDrawScreenPostCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraOverlayRenderEvent>> clientRenderOverlayPostCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> clientMouseClickedPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> clientMouseReleasedPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> clientMouseReleasedPostCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> clientMouseScrolledPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> clientMouseDraggedPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraKeyboardEvent>> clientKeyPressedPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraKeyboardEvent>> clientKeyReleasedPostCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraKeyboardEvent>> clientCharTypedPreCallbacks = new ArrayList<>();

    private static final List<Consumer<BaniraClientTickEvent>> clientTickCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraChatEvent>> clientChatCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraScreenEvent>> clientGuiScreenCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraOverlayRenderEvent>> clientRenderOverlayPreCallbacks = new ArrayList<>();

    private static final List<Consumer<BaniraClientSetupEvent>> modClientSetupCallbacks = new ArrayList<>();

    private static final BaniraMouseClickTracker mouseClickTracker = new BaniraMouseClickTracker();
    private static final BaniraDragTracker dragTracker = new BaniraDragTracker();
    private static final BaniraKeyPressTracker keyPressTracker = new BaniraKeyPressTracker();

    private static volatile boolean codexDefaultsRegistered;

    /**
     * BaniraCodex 在客户端的默认监听
     */
    public static void registerCodexDefaults() {
        if (codexDefaultsRegistered) {
            return;
        }
        codexDefaultsRegistered = true;
        ModLoadedToBoth.registerClientHandler(packet -> {
            LocalPlayer player = BaniraClientRuntime.localPlayer();
            if (player == null) {
                return;
            }
            for (String modid : packet.modids()) {
                PlayerUtils.setRemoteServerModInstalled(player, modid, false);
            }
        });
        Player.onClientLoggedIn(player -> {
            List<String> ids = ModLoadedPresence.announcedModIds();
            if (!ids.isEmpty()) {
                PacketUtils.sendPacketToServer(new ModLoadedToBoth(ids));
            }
        });
        Player.onClientLoggedOut(player -> {
            if (player == null) return;
            AdvancementUtils.clearAdvancementData();
            PlayerUtils.removeRemoteServerDataStatus(player);
        });
        Client.onGuiChanged(event -> {
            resetInputTrackers();
            QuickActionOverlay.get().resetInteractionState();
            LogoModifier.modifyLogo();
        });
        Client.onTextureReload(event -> {
            if (BaniraCodex.MODID.equals(event.atlasLocation().getNamespace())) {
                TextureUtils.resourceReloadEvent();
                QuickActionOverlay.resetSystemIconTextureCache();
            }
        });
        Client.onKeyPressedPre(event -> InputStateManager.instance().handleKeyPressed(event.keyCode()));
        Client.onKeyReleasedPost(event -> InputStateManager.instance().handleKeyReleased(event.keyCode()));
        Client.onClientTick(event -> {
            if (event == BaniraClientTickEvent.END && BaniraClientRuntime.currentScreen() == null) {
                InputStateManager.instance().handleScreenClosed();
            }
        });
    }

    public static void dispatchModClientSetup(BaniraClientSetupEvent event) {
        fire(modClientSetupCallbacks, event, "mod client setup");
    }

    public static void dispatchClientPlayerLoggedIn(net.minecraft.world.entity.player.Player player) {
        fire(clientPlayerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchClientPlayerLoggedOut(net.minecraft.world.entity.player.Player player) {
        fire(clientPlayerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchClientTick(BaniraClientTickEvent event) {
        fire(clientTickCallbacks, event, "client tick");
    }

    public static void dispatchClientChat(BaniraChatEvent event) {
        fire(clientChatCallbacks, event, "client chat");
    }

    public static void dispatchGuiScreen(BaniraScreenEvent event) {
        fire(clientGuiScreenCallbacks, event, "client gui screen");
    }

    public static void dispatchRenderOverlayPre(BaniraOverlayRenderEvent event) {
        fire(clientRenderOverlayPreCallbacks, event, "client render overlay pre");
    }

    public static void dispatchRenderOverlayPreNative(@Nonnull HudOverlayElement element, @Nonnull Object nativeGraphics,
                                                      float partialTick, boolean screenOpen) {
        dispatchRenderOverlayPre(overlayEvent(element, nativeGraphics, partialTick, screenOpen));
    }

    public static void dispatchMouseClickedPre(BaniraMouseEvent event) {
        dispatchMouseClickedPre(event, null);
    }

    public static void dispatchMouseClickedPre(BaniraMouseEvent event, Screen screen) {
        dragTracker.press(event.mouseX(), event.mouseY(), event.button());
        event.withClickMetadata(mouseClickTracker.record(event.mouseX(), event.mouseY(), event.button()));
        handleMouseClickedPre(event, screen);
        fire(clientMouseClickedPreCallbacks, event, "client mouse clicked pre");
    }

    public static void dispatchMouseReleasedPre(BaniraMouseEvent event) {
        dispatchMouseReleasedPre(event, null);
    }

    public static void dispatchMouseReleasedPre(BaniraMouseEvent event, Screen screen) {
        event.withDragMetadata(dragTracker.release(event.mouseX(), event.mouseY(), event.button()));
        handleMouseReleasedPre(event, screen);
        fire(clientMouseReleasedPreCallbacks, event, "client mouse released pre");
    }

    public static void dispatchMouseReleasedPost(BaniraMouseEvent event) {
        handleMouseReleasedPost(event);
        fire(clientMouseReleasedPostCallbacks, event, "client mouse released post");
    }

    public static void dispatchMouseScrolledPre(BaniraMouseEvent event) {
        dispatchMouseScrolledPre(event, null);
    }

    public static void dispatchMouseScrolledPre(BaniraMouseEvent event, Screen screen) {
        handleMouseScrolledPre(event, screen);
        fire(clientMouseScrolledPreCallbacks, event, "client mouse scrolled pre");
    }

    public static void dispatchMouseDraggedPre(BaniraMouseEvent event) {
        dispatchMouseDraggedPre(event, null);
    }

    public static void dispatchMouseDraggedPre(BaniraMouseEvent event, Screen screen) {
        event.withDragMetadata(dragTracker.drag(event.mouseX(), event.mouseY(), event.button(), event.dragX(), event.dragY()));
        fire(clientMouseDraggedPreCallbacks, event, "client mouse dragged pre");
    }

    public static void dispatchKeyPressedPre(BaniraKeyboardEvent event) {
        event.withPressMetadata(keyPressTracker.recordPress(event.keyCode(), event.scanCode(), event.modifiers()));
        fire(clientKeyPressedPreCallbacks, event, "client key pressed pre");
    }

    public static void dispatchKeyReleasedPost(BaniraKeyboardEvent event) {
        keyPressTracker.recordRelease(event.keyCode(), event.scanCode());
        fire(clientKeyReleasedPostCallbacks, event, "client key released post");
    }

    public static void dispatchCharTypedPre(BaniraKeyboardEvent event) {
        fire(clientCharTypedPreCallbacks, event, "client char typed pre");
    }

    // region 分类 API：Player（客户端网络登录/登出）

    public static final class Player {
        private Player() {
        }

        public static void onClientLoggedIn(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            clientPlayerLoggedInCallbacks.add(callback);
        }

        public static void onClientLoggedOut(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            clientPlayerLoggedOutCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：Client

    public static final class Client {
        private Client() {
        }

        public static void onGuiChanged(@Nonnull Consumer<BaniraScreenOpenEvent> callback) {
            clientGuiChangedCallbacks.add(callback);
        }

        public static void fireGuiChanged(BaniraScreenOpenEvent event) {
            fire(clientGuiChangedCallbacks, event, "client gui changed");
        }

        public static void onTextureReload(@Nonnull Consumer<BaniraTextureReloadEvent> callback) {
            clientTextureReloadCallbacks.add(callback);
        }

        public static void onDrawScreenPre(@Nonnull Consumer<BaniraDrawScreenEvent> callback) {
            clientDrawScreenPreCallbacks.add(callback);
        }

        public static void onDrawScreenPost(@Nonnull Consumer<BaniraDrawScreenEvent> callback) {
            clientDrawScreenPostCallbacks.add(callback);
        }

        public static void onRenderOverlayPost(@Nonnull Consumer<BaniraOverlayRenderEvent> callback) {
            clientRenderOverlayPostCallbacks.add(callback);
        }

        public static void onClientTick(@Nonnull Consumer<BaniraClientTickEvent> callback) {
            clientTickCallbacks.add(callback);
        }

        public static void onChat(@Nonnull Consumer<BaniraChatEvent> callback) {
            clientChatCallbacks.add(callback);
        }

        public static void onGuiScreen(@Nonnull Consumer<BaniraScreenEvent> callback) {
            clientGuiScreenCallbacks.add(callback);
        }

        public static void onRenderOverlayPre(@Nonnull Consumer<BaniraOverlayRenderEvent> callback) {
            clientRenderOverlayPreCallbacks.add(callback);
        }

        public static void onMouseClickedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            clientMouseClickedPreCallbacks.add(callback);
        }

        public static void onMouseReleasedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            clientMouseReleasedPreCallbacks.add(callback);
        }

        public static void onMouseReleasedPost(@Nonnull Consumer<BaniraMouseEvent> callback) {
            clientMouseReleasedPostCallbacks.add(callback);
        }

        public static void onMouseScrolledPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            clientMouseScrolledPreCallbacks.add(callback);
        }

        public static void onMouseDraggedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            clientMouseDraggedPreCallbacks.add(callback);
        }

        public static void onKeyPressedPre(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            clientKeyPressedPreCallbacks.add(callback);
        }

        public static void onKeyReleasedPost(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            clientKeyReleasedPostCallbacks.add(callback);
        }

        public static void onCharTypedPre(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            clientCharTypedPreCallbacks.add(callback);
        }

        public static void fireTextureReload(BaniraTextureReloadEvent event) {
            fire(clientTextureReloadCallbacks, event, "client texture reload");
        }

        public static void fireDrawScreenPre(BaniraDrawScreenEvent event) {
            fire(clientDrawScreenPreCallbacks, event, "client draw screen pre");
        }

        public static void fireDrawScreenPost(BaniraDrawScreenEvent event) {
            fire(clientDrawScreenPostCallbacks, event, "client draw screen post");
        }

        public static void fireRenderOverlayPost(BaniraOverlayRenderEvent event) {
            fire(clientRenderOverlayPostCallbacks, event, "client render overlay post");
        }

        public static void fireDrawScreenPreNative(@Nonnull Object nativeGraphics, @Nonnull Screen screen,
                                                   double mouseX, double mouseY, float partialTick) {
            handleDrawScreenPre(screen, mouseX, mouseY);
            fireDrawScreenPre(drawScreenEvent(nativeGraphics, screen, mouseX, mouseY, partialTick));
        }

        public static void fireDrawScreenPostNative(@Nonnull Object nativeGraphics, @Nonnull Screen screen,
                                                    double mouseX, double mouseY, float partialTick) {
            NotificationManager.get().renderNative(nativeGraphics);
            if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
                QuickActionOverlay.get().renderNative(nativeGraphics, screen, mouseX, mouseY, partialTick);
                QuickActionOverlay.get().flushSaveIfNeeded();
            }
            fireDrawScreenPost(drawScreenEvent(nativeGraphics, screen, mouseX, mouseY, partialTick));
        }

        public static void fireRenderOverlayPostNative(@Nonnull HudOverlayElement element, @Nonnull Object nativeGraphics,
                                                       float partialTick, boolean screenOpen) {
            if (element == HudOverlayElement.ALL && !screenOpen) {
                NotificationManager.get().renderNative(nativeGraphics);
            }
            fireRenderOverlayPost(overlayEvent(element, nativeGraphics, partialTick, screenOpen));
        }
    }

    // endregion

    // region 分类 API：ModLifecycle（客户端）

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Consumer<BaniraClientSetupEvent> callback) {
            modClientSetupCallbacks.add(callback);
        }
    }

    // endregion

    // region 内部回调执行

    private static <T> void fire(List<Consumer<T>> callbacks, T parameter, String eventName) {
        for (Consumer<T> callback : callbacks) {
            try {
                callback.accept(parameter);
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for {} event", eventName, t);
            }
        }
    }

    public static BaniraScreenInfo screenInfo(Screen screen) {
        if (screen == null) {
            return BaniraScreenInfo.closed();
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        return new BaniraScreenInfo(screen.getClass().getName(), title, screen.width, screen.height, true);
    }

    private static void resetInputTrackers() {
        mouseClickTracker.reset();
        dragTracker.reset();
        keyPressTracker.reset();
    }

    private static BaniraDrawScreenEvent drawScreenEvent(@Nonnull Object nativeGraphics, @Nonnull Screen screen,
                                                         double mouseX, double mouseY, float partialTick) {
        return new BaniraDrawScreenEvent(drawContext(nativeGraphics, partialTick), screenInfo(screen), mouseX, mouseY, partialTick);
    }

    private static BaniraOverlayRenderEvent overlayEvent(@Nonnull HudOverlayElement element, @Nonnull Object nativeGraphics,
                                                        float partialTick, boolean screenOpen) {
        return new BaniraOverlayRenderEvent(element, hudContext(nativeGraphics, partialTick), partialTick, screenOpen);
    }

    private static BaniraDrawContext drawContext(@Nonnull Object nativeGraphics, float partialTick) {
        KeyValue<Integer, Integer> screen = BaniraClientRuntime.guiScaledSize();
        return new BaniraDrawContext(new PlatformBaniraDrawHandle(nativeGraphics), screen.key(), screen.val(), partialTick);
    }

    private static BaniraHudRenderContext hudContext(@Nonnull Object nativeGraphics, float partialTick) {
        KeyValue<Integer, Integer> screen = BaniraClientRuntime.guiScaledSize();
        return new BaniraHudRenderContext(drawContext(nativeGraphics, partialTick), screen.key(), screen.val(), partialTick);
    }

    private static void handleDrawScreenPre(@Nonnull Screen screen, double mouseX, double mouseY) {
        InputStateManager.instance().handleDrawScreenPre(mouseX, mouseY);
        if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
            QuickActionOverlay.get().tickInteraction(screen, (int) Math.round(mouseX), (int) Math.round(mouseY));
        }
    }

    private static void handleMouseClickedPre(@Nonnull BaniraMouseEvent event, Screen screen) {
        InputStateManager.instance().handleMouseClicked(event.mouseX(), event.mouseY(), event.button());
        if (screen != null && QuickActionOverlay.get().handleMouseClicked(screen, event.mouseX(), event.mouseY(), event.button())) {
            event.cancel();
            return;
        }
        if (NotificationManager.get().tryHandleHudClick(event.mouseX(), event.mouseY(), event.button())) {
            event.cancel();
        }
    }

    private static void handleMouseReleasedPre(@Nonnull BaniraMouseEvent event, Screen screen) {
        if (screen != null && QuickActionOverlay.get().handleMouseReleased(screen, event.mouseX(), event.mouseY(), event.button())) {
            event.cancel();
        }
    }

    private static void handleMouseReleasedPost(@Nonnull BaniraMouseEvent event) {
        InputStateManager.instance().handleMouseReleased(event.mouseX(), event.mouseY(), event.button());
    }

    private static void handleMouseScrolledPre(@Nonnull BaniraMouseEvent event, Screen screen) {
        InputStateManager.instance().handleMouseScrolled(event.mouseX(), event.mouseY(), event.scrollDelta());
        if (screen != null && QuickActionOverlay.get().handleMouseScroll(screen, event.mouseX(), event.mouseY(), event.scrollDelta())) {
            event.cancel();
        }
    }

    // endregion

}
