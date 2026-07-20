package xin.vanilla.banira.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.event.BaniraChatEvent;
import xin.vanilla.banira.api.client.event.BaniraClientEvents;
import xin.vanilla.banira.api.client.event.BaniraClientPlayerEvent;
import xin.vanilla.banira.api.client.event.BaniraClientSetupEvent;
import xin.vanilla.banira.api.client.event.BaniraClientTickEvent;
import xin.vanilla.banira.api.client.event.BaniraDrawScreenEvent;
import xin.vanilla.banira.api.client.event.BaniraKeyboardEvent;
import xin.vanilla.banira.api.client.event.BaniraMouseEvent;
import xin.vanilla.banira.api.client.event.BaniraOverlayRenderEvent;
import xin.vanilla.banira.api.client.event.BaniraScreenEvent;
import xin.vanilla.banira.api.client.event.BaniraScreenInfo;
import xin.vanilla.banira.api.client.event.BaniraScreenOpenEvent;
import xin.vanilla.banira.api.client.event.BaniraTextureReloadEvent;
import xin.vanilla.banira.api.client.hud.BaniraHudRenderContext;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.api.client.input.BaniraDragTracker;
import xin.vanilla.banira.api.client.input.BaniraKeyPressTracker;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;
import xin.vanilla.banira.api.client.render.BaniraDrawContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.internal.client.BaniraClientAccess;
import xin.vanilla.banira.internal.client.BaniraClientDefaults;
import xin.vanilla.banira.internal.client.BaniraLegacyDrawHandle;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 客户端公共事件入口；1.16.5 的 Forge/PoseStack 差异只在 adapter 内转换。
 */
public final class BaniraClientEventHub {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<net.minecraft.world.entity.player.Player>> clientPlayerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraScreenOpenEvent>> clientGuiChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraTextureReloadEvent>> clientTextureReloadCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraDrawScreenEvent>> clientDrawScreenPreCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraDrawScreenEvent>> clientDrawScreenPostCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraOverlayRenderEvent>> clientRenderOverlayPreCallbacks = new ArrayList<>();
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
    private static final List<Consumer<BaniraClientSetupEvent>> modClientSetupCallbacks = new ArrayList<>();

    private static final BaniraMouseClickTracker mouseClickTracker = new BaniraMouseClickTracker();
    private static final BaniraDragTracker dragTracker = new BaniraDragTracker();
    private static final BaniraKeyPressTracker keyPressTracker = new BaniraKeyPressTracker();
    private static volatile boolean codexDefaultsRegistered;

    private BaniraClientEventHub() {
    }

    public static void registerCodexDefaults() {
        if (codexDefaultsRegistered) {
            return;
        }
        codexDefaultsRegistered = true;
        BaniraClientDefaults.register();
    }

    public static void dispatchModClientSetup() {
        dispatchModClientSetup(new BaniraClientSetupEvent());
    }

    public static void dispatchModClientSetup(@Nonnull BaniraClientSetupEvent event) {
        BaniraClientEvents.dispatchModClientSetup(event);
        fire(modClientSetupCallbacks, event, "mod client setup");
    }

    public static void dispatchClientPlayerLoggedIn(net.minecraft.world.entity.player.Player player) {
        BaniraClientEvents.dispatchClientPlayerLoggedIn(playerEvent(player));
        fire(clientPlayerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchClientPlayerLoggedOut(net.minecraft.world.entity.player.Player player) {
        BaniraClientEvents.dispatchClientPlayerLoggedOut(playerEvent(player));
        fire(clientPlayerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchClientTick(@Nonnull BaniraClientTickEvent event) {
        BaniraClientEvents.dispatchClientTick(event);
        fire(clientTickCallbacks, event, "client tick");
    }

    public static void dispatchClientChat(@Nonnull BaniraChatEvent event) {
        BaniraClientEvents.dispatchClientChat(event);
        fire(clientChatCallbacks, event, "client chat");
    }

    public static void dispatchGuiScreen(@Nonnull BaniraScreenEvent event) {
        BaniraClientEvents.dispatchGuiScreen(event);
        fire(clientGuiScreenCallbacks, event, "client gui screen");
    }

    public static void dispatchRenderOverlayPre(@Nonnull BaniraOverlayRenderEvent event) {
        BaniraClientEvents.dispatchRenderOverlayPre(event);
        fire(clientRenderOverlayPreCallbacks, event, "client render overlay pre");
    }

    public static void dispatchRenderOverlayPreNative(@Nonnull HudOverlayElement element, @Nonnull PoseStack nativeGraphics,
                                                      float partialTick, boolean screenOpen) {
        dispatchRenderOverlayPre(overlayEvent(element, nativeGraphics, partialTick, screenOpen));
    }

    public static void dispatchMouseClickedPre(@Nonnull BaniraMouseEvent event, Screen screen) {
        dragTracker.press(event.mouseX(), event.mouseY(), event.button());
        event.withClickMetadata(mouseClickTracker.record(event.mouseX(), event.mouseY(), event.button()));
        BaniraClientEvents.dispatchMouseClickedPre(event);
        fire(clientMouseClickedPreCallbacks, event, "client mouse clicked pre");
    }

    public static void dispatchMouseReleasedPre(@Nonnull BaniraMouseEvent event, Screen screen) {
        event.withDragMetadata(dragTracker.release(event.mouseX(), event.mouseY(), event.button()));
        BaniraClientEvents.dispatchMouseReleasedPre(event);
        fire(clientMouseReleasedPreCallbacks, event, "client mouse released pre");
    }

    public static void dispatchMouseReleasedPost(@Nonnull BaniraMouseEvent event) {
        BaniraClientEvents.dispatchMouseReleasedPost(event);
        fire(clientMouseReleasedPostCallbacks, event, "client mouse released post");
    }

    public static void dispatchMouseScrolledPre(@Nonnull BaniraMouseEvent event, Screen screen) {
        BaniraClientEvents.dispatchMouseScrolledPre(event);
        fire(clientMouseScrolledPreCallbacks, event, "client mouse scrolled pre");
    }

    public static void dispatchMouseDraggedPre(@Nonnull BaniraMouseEvent event, Screen screen) {
        event.withDragMetadata(dragTracker.drag(event.mouseX(), event.mouseY(), event.button(), event.dragX(), event.dragY()));
        BaniraClientEvents.dispatchMouseDraggedPre(event);
        fire(clientMouseDraggedPreCallbacks, event, "client mouse dragged pre");
    }

    public static void dispatchKeyPressedPre(@Nonnull BaniraKeyboardEvent event) {
        event.withPressMetadata(keyPressTracker.recordPress(event.keyCode(), event.scanCode(), event.modifiers()));
        BaniraClientEvents.dispatchKeyPressedPre(event);
        fire(clientKeyPressedPreCallbacks, event, "client key pressed pre");
    }

    public static void dispatchKeyReleasedPost(@Nonnull BaniraKeyboardEvent event) {
        keyPressTracker.recordRelease(event.keyCode(), event.scanCode());
        BaniraClientEvents.dispatchKeyReleasedPost(event);
        fire(clientKeyReleasedPostCallbacks, event, "client key released post");
    }

    public static void dispatchCharTypedPre(@Nonnull BaniraKeyboardEvent event) {
        BaniraClientEvents.dispatchCharTypedPre(event);
        fire(clientCharTypedPreCallbacks, event, "client char typed pre");
    }

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

    public static final class Client {
        private Client() {
        }

        public static void onGuiChanged(@Nonnull Consumer<BaniraScreenOpenEvent> callback) {
            clientGuiChangedCallbacks.add(callback);
        }

        public static void fireGuiChanged(@Nonnull BaniraScreenOpenEvent event) {
            resetInputTrackers();
            BaniraClientEvents.Client.fireGuiChanged(event);
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

        public static void onRenderOverlayPre(@Nonnull Consumer<BaniraOverlayRenderEvent> callback) {
            clientRenderOverlayPreCallbacks.add(callback);
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

        public static void fireTextureReload(@Nonnull BaniraTextureReloadEvent event) {
            BaniraClientEvents.Client.fireTextureReload(event);
            fire(clientTextureReloadCallbacks, event, "client texture reload");
        }

        public static void fireDrawScreenPre(@Nonnull BaniraDrawScreenEvent event) {
            BaniraClientEvents.Client.fireDrawScreenPre(event);
            fire(clientDrawScreenPreCallbacks, event, "client draw screen pre");
        }

        public static void fireDrawScreenPost(@Nonnull BaniraDrawScreenEvent event) {
            BaniraClientEvents.Client.fireDrawScreenPost(event);
            fire(clientDrawScreenPostCallbacks, event, "client draw screen post");
        }

        public static void fireRenderOverlayPost(@Nonnull BaniraOverlayRenderEvent event) {
            BaniraClientEvents.Client.fireRenderOverlayPost(event);
            fire(clientRenderOverlayPostCallbacks, event, "client render overlay post");
        }

        public static void fireDrawScreenPreNative(@Nonnull PoseStack nativeGraphics, @Nonnull Screen screen,
                                                   double mouseX, double mouseY, float partialTick) {
            fireDrawScreenPre(drawScreenEvent(nativeGraphics, screen, mouseX, mouseY, partialTick));
        }

        public static void fireDrawScreenPostNative(@Nonnull PoseStack nativeGraphics, @Nonnull Screen screen,
                                                    double mouseX, double mouseY, float partialTick) {
            if (QuickActionOverlay.isSupportedInventoryScreen(screen)) {
                QuickActionOverlay.get().render(nativeGraphics, screen, (int) mouseX, (int) mouseY, partialTick);
                QuickActionOverlay.get().flushSaveIfNeeded();
            }
            NotificationManager.get().render(nativeGraphics);
            fireDrawScreenPost(drawScreenEvent(nativeGraphics, screen, mouseX, mouseY, partialTick));
        }

        public static void fireRenderOverlayPostNative(@Nonnull HudOverlayElement element, @Nonnull PoseStack nativeGraphics,
                                                       float partialTick, boolean screenOpen) {
            if (element == HudOverlayElement.ALL && !screenOpen) {
                NotificationManager.get().render(nativeGraphics);
            }
            fireRenderOverlayPost(overlayEvent(element, nativeGraphics, partialTick, screenOpen));
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Consumer<BaniraClientSetupEvent> callback) {
            modClientSetupCallbacks.add(callback);
        }
    }

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
        return new BaniraScreenInfo(
                screen.getClass().getName(),
                screen.getTitle() == null ? "" : screen.getTitle().getString(),
                screen.width,
                screen.height,
                true
        );
    }

    public static void resetInputTrackers() {
        mouseClickTracker.reset();
        dragTracker.reset();
        keyPressTracker.reset();
        BaniraClientEvents.resetInputTrackers();
    }

    private static BaniraClientPlayerEvent playerEvent(net.minecraft.world.entity.player.Player player) {
        return player == null
                ? new BaniraClientPlayerEvent(new java.util.UUID(0L, 0L), "")
                : new BaniraClientPlayerEvent(player.getUUID(), player.getName().getString());
    }

    private static BaniraDrawScreenEvent drawScreenEvent(@Nonnull PoseStack nativeGraphics, @Nonnull Screen screen,
                                                         double mouseX, double mouseY, float partialTick) {
        return new BaniraDrawScreenEvent(drawContext(nativeGraphics, partialTick), screenInfo(screen), mouseX, mouseY, partialTick);
    }

    private static BaniraOverlayRenderEvent overlayEvent(@Nonnull HudOverlayElement element, @Nonnull PoseStack nativeGraphics,
                                                        float partialTick, boolean screenOpen) {
        return new BaniraOverlayRenderEvent(element, hudContext(nativeGraphics, partialTick), partialTick, screenOpen);
    }

    private static BaniraDrawContext drawContext(@Nonnull PoseStack nativeGraphics, float partialTick) {
        KeyValue<Integer, Integer> screen = BaniraClientAccess.guiScaledSize();
        return new BaniraDrawContext(new BaniraLegacyDrawHandle(nativeGraphics), screen.key(), screen.val(), partialTick);
    }

    private static BaniraHudRenderContext hudContext(@Nonnull PoseStack nativeGraphics, float partialTick) {
        KeyValue<Integer, Integer> screen = BaniraClientAccess.guiScaledSize();
        return new BaniraHudRenderContext(drawContext(nativeGraphics, partialTick), screen.key(), screen.val(), partialTick);
    }
}
