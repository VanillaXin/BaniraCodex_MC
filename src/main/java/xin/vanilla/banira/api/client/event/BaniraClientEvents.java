package xin.vanilla.banira.api.client.event;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.input.BaniraDragTracker;
import xin.vanilla.banira.api.client.input.BaniraKeyPressTracker;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 子 mod 注册客户端事件的稳定入口；加载器事件由 Banira 内部适配层转换。
 */
public final class BaniraClientEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<net.minecraft.world.entity.player.Player>> CLIENT_PLAYER_LOGGED_IN = new CopyOnWriteArrayList<>();
    private static final List<Consumer<net.minecraft.world.entity.player.Player>> CLIENT_PLAYER_LOGGED_OUT = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraScreenOpenEvent>> GUI_CHANGED = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraTextureReloadEvent>> TEXTURE_RELOAD = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraDrawScreenEvent>> DRAW_SCREEN_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraDrawScreenEvent>> DRAW_SCREEN_POST = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraOverlayRenderEvent>> RENDER_OVERLAY_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraOverlayRenderEvent>> RENDER_OVERLAY_POST = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> MOUSE_CLICKED_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> MOUSE_RELEASED_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> MOUSE_RELEASED_POST = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> MOUSE_SCROLLED_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraMouseEvent>> MOUSE_DRAGGED_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraKeyboardEvent>> KEY_PRESSED_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraKeyboardEvent>> KEY_RELEASED_POST = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraKeyboardEvent>> CHAR_TYPED_PRE = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraClientTickEvent>> CLIENT_TICK = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraChatEvent>> CHAT = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraScreenEvent>> GUI_SCREEN = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraClientSetupEvent>> CLIENT_SETUP = new CopyOnWriteArrayList<>();

    private static final BaniraMouseClickTracker MOUSE_CLICK_TRACKER = new BaniraMouseClickTracker();
    private static final BaniraDragTracker DRAG_TRACKER = new BaniraDragTracker();
    private static final BaniraKeyPressTracker KEY_PRESS_TRACKER = new BaniraKeyPressTracker();

    private BaniraClientEvents() {
    }

    public static void resetInputTrackers() {
        MOUSE_CLICK_TRACKER.reset();
        DRAG_TRACKER.reset();
        KEY_PRESS_TRACKER.reset();
    }

    public static void dispatchModClientSetup(@Nonnull BaniraClientSetupEvent event) {
        fire(CLIENT_SETUP, event, "mod client setup");
    }

    public static void dispatchClientPlayerLoggedIn(@Nonnull net.minecraft.world.entity.player.Player player) {
        fire(CLIENT_PLAYER_LOGGED_IN, player, "player logged in");
    }

    public static void dispatchClientPlayerLoggedOut(@Nonnull net.minecraft.world.entity.player.Player player) {
        fire(CLIENT_PLAYER_LOGGED_OUT, player, "player logged out");
    }

    public static void dispatchClientTick(@Nonnull BaniraClientTickEvent event) {
        fire(CLIENT_TICK, event, "client tick");
    }

    public static void dispatchClientChat(@Nonnull BaniraChatEvent event) {
        fire(CHAT, event, "client chat");
    }

    public static void dispatchGuiScreen(@Nonnull BaniraScreenEvent event) {
        fire(GUI_SCREEN, event, "client gui screen");
    }

    public static void dispatchRenderOverlayPre(@Nonnull BaniraOverlayRenderEvent event) {
        fire(RENDER_OVERLAY_PRE, event, "client render overlay pre");
    }

    public static void dispatchMouseClickedPre(@Nonnull BaniraMouseEvent event) {
        DRAG_TRACKER.press(event.mouseX(), event.mouseY(), event.button());
        event.withClickMetadata(MOUSE_CLICK_TRACKER.record(event.mouseX(), event.mouseY(), event.button()));
        fire(MOUSE_CLICKED_PRE, event, "client mouse clicked pre");
    }

    public static void dispatchMouseReleasedPre(@Nonnull BaniraMouseEvent event) {
        event.withDragMetadata(DRAG_TRACKER.release(event.mouseX(), event.mouseY(), event.button()));
        fire(MOUSE_RELEASED_PRE, event, "client mouse released pre");
    }

    public static void dispatchMouseReleasedPost(@Nonnull BaniraMouseEvent event) {
        fire(MOUSE_RELEASED_POST, event, "client mouse released post");
    }

    public static void dispatchMouseScrolledPre(@Nonnull BaniraMouseEvent event) {
        fire(MOUSE_SCROLLED_PRE, event, "client mouse scrolled pre");
    }

    public static void dispatchMouseDraggedPre(@Nonnull BaniraMouseEvent event) {
        event.withDragMetadata(DRAG_TRACKER.drag(event.mouseX(), event.mouseY(), event.button(), event.dragX(), event.dragY()));
        fire(MOUSE_DRAGGED_PRE, event, "client mouse dragged pre");
    }

    public static void dispatchKeyPressedPre(@Nonnull BaniraKeyboardEvent event) {
        event.withPressMetadata(KEY_PRESS_TRACKER.recordPress(event.keyCode(), event.scanCode(), event.modifiers()));
        fire(KEY_PRESSED_PRE, event, "client key pressed pre");
    }

    public static void dispatchKeyReleasedPost(@Nonnull BaniraKeyboardEvent event) {
        KEY_PRESS_TRACKER.recordRelease(event.keyCode(), event.scanCode());
        fire(KEY_RELEASED_POST, event, "client key released post");
    }

    public static void dispatchCharTypedPre(@Nonnull BaniraKeyboardEvent event) {
        fire(CHAR_TYPED_PRE, event, "client char typed pre");
    }

    public static final class Player {
        private Player() {
        }

        public static void onClientLoggedIn(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            CLIENT_PLAYER_LOGGED_IN.add(callback);
        }

        public static void onClientLoggedOut(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            CLIENT_PLAYER_LOGGED_OUT.add(callback);
        }
    }

    public static final class Client {
        private Client() {
        }

        public static void onGuiChanged(@Nonnull Consumer<BaniraScreenOpenEvent> callback) {
            GUI_CHANGED.add(callback);
        }

        public static void fireGuiChanged(@Nonnull BaniraScreenOpenEvent event) {
            fire(GUI_CHANGED, event, "client gui changed");
        }

        public static void onTextureReload(@Nonnull Consumer<BaniraTextureReloadEvent> callback) {
            TEXTURE_RELOAD.add(callback);
        }

        public static void fireTextureReload(@Nonnull BaniraTextureReloadEvent event) {
            fire(TEXTURE_RELOAD, event, "client texture reload");
        }

        public static void onDrawScreenPre(@Nonnull Consumer<BaniraDrawScreenEvent> callback) {
            DRAW_SCREEN_PRE.add(callback);
        }

        public static void fireDrawScreenPre(@Nonnull BaniraDrawScreenEvent event) {
            fire(DRAW_SCREEN_PRE, event, "client draw screen pre");
        }

        public static void onDrawScreenPost(@Nonnull Consumer<BaniraDrawScreenEvent> callback) {
            DRAW_SCREEN_POST.add(callback);
        }

        public static void fireDrawScreenPost(@Nonnull BaniraDrawScreenEvent event) {
            fire(DRAW_SCREEN_POST, event, "client draw screen post");
        }

        public static void onRenderOverlayPre(@Nonnull Consumer<BaniraOverlayRenderEvent> callback) {
            RENDER_OVERLAY_PRE.add(callback);
        }

        public static void onRenderOverlayPost(@Nonnull Consumer<BaniraOverlayRenderEvent> callback) {
            RENDER_OVERLAY_POST.add(callback);
        }

        public static void fireRenderOverlayPost(@Nonnull BaniraOverlayRenderEvent event) {
            fire(RENDER_OVERLAY_POST, event, "client render overlay post");
        }

        public static void onClientTick(@Nonnull Consumer<BaniraClientTickEvent> callback) {
            CLIENT_TICK.add(callback);
        }

        public static void onChat(@Nonnull Consumer<BaniraChatEvent> callback) {
            CHAT.add(callback);
        }

        public static void onGuiScreen(@Nonnull Consumer<BaniraScreenEvent> callback) {
            GUI_SCREEN.add(callback);
        }

        public static void onMouseClickedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            MOUSE_CLICKED_PRE.add(callback);
        }

        public static void onMouseReleasedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            MOUSE_RELEASED_PRE.add(callback);
        }

        public static void onMouseReleasedPost(@Nonnull Consumer<BaniraMouseEvent> callback) {
            MOUSE_RELEASED_POST.add(callback);
        }

        public static void onMouseScrolledPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            MOUSE_SCROLLED_PRE.add(callback);
        }

        public static void onMouseDraggedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            MOUSE_DRAGGED_PRE.add(callback);
        }

        public static void onKeyPressedPre(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            KEY_PRESSED_PRE.add(callback);
        }

        public static void onKeyReleasedPost(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            KEY_RELEASED_POST.add(callback);
        }

        public static void onCharTypedPre(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            CHAR_TYPED_PRE.add(callback);
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Consumer<BaniraClientSetupEvent> callback) {
            CLIENT_SETUP.add(callback);
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
}
