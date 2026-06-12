package xin.vanilla.banira.client.event;

import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.internal.client.BaniraClientDefaults;
import xin.vanilla.banira.internal.client.BaniraLegacyDrawHandle;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client-only event hub exposed to child mods. Loader event objects are converted
 * by internal adapters before reaching this class.
 */
public final class BaniraClientEventHub {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<PlayerEntity>> clientPlayerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEntity>> clientPlayerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientScreenEvent>> clientScreenChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraTextureReloadEvent>> clientTextureReloadCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientScreenEvent>> clientScreenPreRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientScreenEvent>> clientScreenPostRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientInputEvent>> clientInputCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraHudRenderEvent>> hudPreRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraHudRenderEvent>> hudPostRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientTickEvent>> clientTickCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientChatEvent>> clientChatCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraClientScreenEvent>> clientScreenCallbacks = new ArrayList<>();
    private static final List<Runnable> modClientSetupCallbacks = new ArrayList<>();

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
        fire(modClientSetupCallbacks, "mod client setup");
    }

    public static void dispatchClientPlayerLoggedIn(PlayerEntity player) {
        fire(clientPlayerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchClientPlayerLoggedOut(PlayerEntity player) {
        fire(clientPlayerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchClientTick(BaniraClientTickEvent event) {
        fire(clientTickCallbacks, event, "client tick");
    }

    public static void dispatchClientChat(BaniraClientChatEvent event) {
        fire(clientChatCallbacks, event, "client chat");
    }

    public static void dispatchClientScreen(BaniraClientScreenEvent event) {
        fire(clientScreenCallbacks, event, "client screen");
    }

    public static void dispatchClientScreenChanged(BaniraClientScreenEvent event) {
        fire(clientScreenChangedCallbacks, event, "client screen changed");
    }

    public static void dispatchClientScreenPreRender(BaniraClientScreenEvent event) {
        fire(clientScreenPreRenderCallbacks, event, "client screen pre render");
    }

    public static void dispatchClientTextureReload(BaniraTextureReloadEvent event) {
        fire(clientTextureReloadCallbacks, event, "client texture reload");
    }

    public static void dispatchClientScreenPostRender(BaniraClientScreenEvent event) {
        fire(clientScreenPostRenderCallbacks, event, "client screen post render");
    }

    public static void dispatchClientInput(BaniraClientInputEvent event) {
        fire(clientInputCallbacks, event, "client input");
    }

    public static void dispatchHudPreRender(BaniraHudRenderEvent event) {
        fire(hudPreRenderCallbacks, event, "hud pre render");
        dispatchApiHudEvent(event, true);
    }

    public static void dispatchHudPostRender(BaniraHudRenderEvent event) {
        fire(hudPostRenderCallbacks, event, "hud post render");
        dispatchApiHudEvent(event, false);
    }

    public static final class Player {
        private Player() {
        }

        public static void onClientLoggedIn(@Nonnull Consumer<PlayerEntity> callback) {
            clientPlayerLoggedInCallbacks.add(callback);
        }

        public static void onClientLoggedOut(@Nonnull Consumer<PlayerEntity> callback) {
            clientPlayerLoggedOutCallbacks.add(callback);
        }
    }

    public static final class Client {
        private Client() {
        }

        public static void onScreenChanged(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenChangedCallbacks.add(callback);
        }

        public static void onTextureReload(@Nonnull Consumer<BaniraTextureReloadEvent> callback) {
            clientTextureReloadCallbacks.add(callback);
        }

        public static void onScreenPreRender(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenPreRenderCallbacks.add(callback);
        }

        public static void onScreenPostRender(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenPostRenderCallbacks.add(callback);
        }

        public static void onClientTick(@Nonnull Consumer<BaniraClientTickEvent> callback) {
            clientTickCallbacks.add(callback);
        }

        public static void onChat(@Nonnull Consumer<BaniraClientChatEvent> callback) {
            clientChatCallbacks.add(callback);
        }

        public static void onScreenEvent(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenCallbacks.add(callback);
        }
    }

    public static final class Screen {
        private Screen() {
        }

        public static void onChanged(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenChangedCallbacks.add(callback);
        }

        public static void onEvent(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenCallbacks.add(callback);
        }

        public static void onPreRender(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenPreRenderCallbacks.add(callback);
        }

        public static void onPostRender(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenPostRenderCallbacks.add(callback);
        }
    }

    public static final class Input {
        private Input() {
        }

        public static void onInput(@Nonnull Consumer<BaniraClientInputEvent> callback) {
            clientInputCallbacks.add(callback);
        }
    }

    public static final class Hud {
        private Hud() {
        }

        public static void onPreRender(@Nonnull Consumer<BaniraHudRenderEvent> callback) {
            hudPreRenderCallbacks.add(callback);
        }

        public static void onPostRender(@Nonnull Consumer<BaniraHudRenderEvent> callback) {
            hudPostRenderCallbacks.add(callback);
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onClientSetup(@Nonnull Runnable callback) {
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

    private static void fire(List<Runnable> callbacks, String eventName) {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for {} event", eventName, t);
            }
        }
    }

    private static void dispatchApiHudEvent(BaniraHudRenderEvent event, boolean pre) {
        if (event == null) {
            return;
        }
        // 新 HUD API 的取消结果需要回写给 1.16.5 的 Forge 事件适配层。
        xin.vanilla.banira.api.client.hud.BaniraHudRenderEvent apiEvent = toApiHudEvent(event, pre);
        if (pre) {
            xin.vanilla.banira.api.client.hud.BaniraHudEvents.dispatchPre(apiEvent);
        } else {
            xin.vanilla.banira.api.client.hud.BaniraHudEvents.dispatchPost(apiEvent);
        }
        if (apiEvent.canceled()) {
            event.cancelVanilla();
        }
    }

    private static xin.vanilla.banira.api.client.hud.BaniraHudRenderEvent toApiHudEvent(BaniraHudRenderEvent event, boolean pre) {
        BaniraDrawContext legacyDraw = event.draw();
        xin.vanilla.banira.api.client.render.BaniraDrawContext draw =
                new xin.vanilla.banira.api.client.render.BaniraDrawContext(
                        new BaniraLegacyDrawHandle(legacyDraw),
                        legacyDraw != null ? legacyDraw.width() : 0,
                        legacyDraw != null ? legacyDraw.height() : 0,
                        legacyDraw != null ? legacyDraw.partialTicks() : 0.0F
                );
        xin.vanilla.banira.api.client.hud.BaniraHudRenderContext context =
                new xin.vanilla.banira.api.client.hud.BaniraHudRenderContext(
                        draw,
                        draw.screenWidth(),
                        draw.screenHeight(),
                        draw.partialTick()
                );
        return new xin.vanilla.banira.api.client.hud.BaniraHudRenderEvent(
                pre ? xin.vanilla.banira.api.client.hud.HudRenderPhase.PRE : xin.vanilla.banira.api.client.hud.HudRenderPhase.POST,
                toApiHudElement(event.element()),
                context,
                toApiHudBounds(event.bounds()),
                pre
        );
    }

    private static xin.vanilla.banira.api.client.hud.BaniraHudBounds toApiHudBounds(BaniraHudBounds bounds) {
        if (bounds == null || !bounds.isKnown()) {
            return xin.vanilla.banira.api.client.hud.BaniraHudBounds.empty();
        }
        return xin.vanilla.banira.api.client.hud.BaniraHudBounds.of(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static xin.vanilla.banira.api.client.hud.HudOverlayElement toApiHudElement(BaniraHudOverlayElement element) {
        if (element == null) {
            return xin.vanilla.banira.api.client.hud.HudOverlayElement.UNKNOWN;
        }
        switch (element) {
            case ALL:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.ALL;
            case HOTBAR:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.HOTBAR;
            case EXPERIENCE_BAR:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.EXPERIENCE_BAR;
            case EXPERIENCE_TEXT:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.EXPERIENCE_TEXT;
            case HEALTH:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.HEALTH;
            case ARMOR:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.ARMOR;
            case FOOD:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.FOOD;
            case AIR:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.AIR;
            case CHAT:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.CHAT;
            case CROSSHAIR:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.CROSSHAIR;
            case BOSS_HEALTH:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.BOSS_HEALTH;
            case PLAYER_LIST:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.PLAYER_LIST;
            case DEBUG:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.DEBUG_TEXT;
            case HUD_TEXT:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.TEXT;
            default:
                return xin.vanilla.banira.api.client.hud.HudOverlayElement.UNKNOWN;
        }
    }
}
