package xin.vanilla.banira.api.client.event;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 子 mod 注册客户端事件的稳定入口；低版本 Forge 分支转调既有的客户端事件 Hub。
 */
public final class BaniraClientEvents {
    private static final List<Consumer<BaniraClientPlayerEvent>> CLIENT_PLAYER_LOGGED_IN = new CopyOnWriteArrayList<>();
    private static final List<Consumer<BaniraClientPlayerEvent>> CLIENT_PLAYER_LOGGED_OUT = new CopyOnWriteArrayList<>();

    private BaniraClientEvents() {
    }

    public static void resetInputTrackers() {
        xin.vanilla.banira.client.event.BaniraClientEventHub.resetInputTrackers();
    }

    public static void dispatchModClientSetup(@Nonnull BaniraClientSetupEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchModClientSetup(event);
    }

    public static void dispatchClientPlayerLoggedIn(@Nonnull BaniraClientPlayerEvent event) {
        fire(CLIENT_PLAYER_LOGGED_IN, event);
    }

    public static void dispatchClientPlayerLoggedOut(@Nonnull BaniraClientPlayerEvent event) {
        fire(CLIENT_PLAYER_LOGGED_OUT, event);
    }

    public static void dispatchClientTick(@Nonnull BaniraClientTickEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchClientTick(event);
    }

    public static void dispatchClientChat(@Nonnull BaniraChatEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchClientChat(event);
    }

    public static void dispatchGuiScreen(@Nonnull BaniraScreenEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchGuiScreen(event);
    }

    public static void dispatchRenderOverlayPre(@Nonnull BaniraOverlayRenderEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchRenderOverlayPre(event);
    }

    public static void dispatchMouseClickedPre(@Nonnull BaniraMouseEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchMouseClickedPre(event, null);
    }

    public static void dispatchMouseReleasedPre(@Nonnull BaniraMouseEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchMouseReleasedPre(event, null);
    }

    public static void dispatchMouseReleasedPost(@Nonnull BaniraMouseEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchMouseReleasedPost(event);
    }

    public static void dispatchMouseScrolledPre(@Nonnull BaniraMouseEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchMouseScrolledPre(event, null);
    }

    public static void dispatchMouseDraggedPre(@Nonnull BaniraMouseEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchMouseDraggedPre(event, null);
    }

    public static void dispatchKeyPressedPre(@Nonnull BaniraKeyboardEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchKeyPressedPre(event);
    }

    public static void dispatchKeyReleasedPost(@Nonnull BaniraKeyboardEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchKeyReleasedPost(event);
    }

    public static void dispatchCharTypedPre(@Nonnull BaniraKeyboardEvent event) {
        xin.vanilla.banira.client.event.BaniraClientEventHub.dispatchCharTypedPre(event);
    }

    public static final class Player {
        private Player() {
        }

        public static void onClientLoggedIn(@Nonnull Consumer<BaniraClientPlayerEvent> callback) {
            CLIENT_PLAYER_LOGGED_IN.add(callback);
            xin.vanilla.banira.client.event.BaniraClientEventHub.Player.onClientLoggedIn(player -> callback.accept(playerEvent(player)));
        }

        public static void onClientLoggedOut(@Nonnull Consumer<BaniraClientPlayerEvent> callback) {
            CLIENT_PLAYER_LOGGED_OUT.add(callback);
            xin.vanilla.banira.client.event.BaniraClientEventHub.Player.onClientLoggedOut(player -> callback.accept(playerEvent(player)));
        }
    }

    public static final class Client {
        private Client() {
        }

        public static void onGuiChanged(@Nonnull Consumer<BaniraScreenOpenEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onGuiChanged(callback);
        }

        public static void fireGuiChanged(@Nonnull BaniraScreenOpenEvent event) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.fireGuiChanged(event);
        }

        public static void onTextureReload(@Nonnull Consumer<BaniraTextureReloadEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onTextureReload(callback);
        }

        public static void fireTextureReload(@Nonnull BaniraTextureReloadEvent event) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.fireTextureReload(event);
        }

        public static void onDrawScreenPre(@Nonnull Consumer<BaniraDrawScreenEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onDrawScreenPre(callback);
        }

        public static void fireDrawScreenPre(@Nonnull BaniraDrawScreenEvent event) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.fireDrawScreenPre(event);
        }

        public static void onDrawScreenPost(@Nonnull Consumer<BaniraDrawScreenEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onDrawScreenPost(callback);
        }

        public static void fireDrawScreenPost(@Nonnull BaniraDrawScreenEvent event) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.fireDrawScreenPost(event);
        }

        public static void onRenderOverlayPre(@Nonnull Consumer<BaniraOverlayRenderEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onRenderOverlayPre(callback);
        }

        public static void onRenderOverlayPost(@Nonnull Consumer<BaniraOverlayRenderEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onRenderOverlayPost(callback);
        }

        public static void fireRenderOverlayPost(@Nonnull BaniraOverlayRenderEvent event) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.fireRenderOverlayPost(event);
        }

        public static void onClientTick(@Nonnull Consumer<BaniraClientTickEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onClientTick(callback);
        }

        public static void onChat(@Nonnull Consumer<BaniraChatEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onChat(callback);
        }

        public static void onGuiScreen(@Nonnull Consumer<BaniraScreenEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onGuiScreen(callback);
        }

        public static void onMouseClickedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onMouseClickedPre(callback);
        }

        public static void onMouseReleasedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onMouseReleasedPre(callback);
        }

        public static void onMouseReleasedPost(@Nonnull Consumer<BaniraMouseEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onMouseReleasedPost(callback);
        }

        public static void onMouseScrolledPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onMouseScrolledPre(callback);
        }

        public static void onMouseDraggedPre(@Nonnull Consumer<BaniraMouseEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onMouseDraggedPre(callback);
        }

        public static void onKeyPressedPre(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onKeyPressedPre(callback);
        }

        public static void onKeyReleasedPost(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onKeyReleasedPost(callback);
        }

        public static void onCharTypedPre(@Nonnull Consumer<BaniraKeyboardEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.Client.onCharTypedPre(callback);
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Consumer<BaniraClientSetupEvent> callback) {
            xin.vanilla.banira.client.event.BaniraClientEventHub.ModLifecycle.onClientSetup(callback);
        }
    }

    private static BaniraClientPlayerEvent playerEvent(Object player) {
        if (player == null) {
            return new BaniraClientPlayerEvent(new UUID(0L, 0L), "");
        }
        UUID uuid = invokeUuid(player);
        String name = invokeName(player);
        return new BaniraClientPlayerEvent(uuid, name);
    }

    private static void fire(List<Consumer<BaniraClientPlayerEvent>> callbacks, BaniraClientPlayerEvent event) {
        for (Consumer<BaniraClientPlayerEvent> callback : callbacks) {
            callback.accept(event);
        }
    }

    private static UUID invokeUuid(Object player) {
        try {
            Method method = player.getClass().getMethod("getUUID");
            Object value = method.invoke(player);
            if (value instanceof UUID) {
                return (UUID) value;
            }
        } catch (ReflectiveOperationException ignored) {
            // 不同 MC 版本玩家类名不同，失败时使用空 UUID，避免公开 API 暴露 native Player。
        }
        return new UUID(0L, 0L);
    }

    private static String invokeName(Object player) {
        try {
            Method method = player.getClass().getMethod("getName");
            Object value = method.invoke(player);
            if (value == null) {
                return "";
            }
            try {
                Method getString = value.getClass().getMethod("getString");
                Object text = getString.invoke(value);
                return text == null ? "" : String.valueOf(text);
            } catch (ReflectiveOperationException ignored) {
                return String.valueOf(value);
            }
        } catch (ReflectiveOperationException ignored) {
            return String.valueOf(player);
        }
    }
}
