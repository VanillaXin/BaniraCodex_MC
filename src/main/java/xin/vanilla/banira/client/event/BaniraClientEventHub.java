package xin.vanilla.banira.client.event;

import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.client.input.BaniraDragTracker;
import xin.vanilla.banira.api.client.input.BaniraKeyPressTracker;
import xin.vanilla.banira.api.client.input.BaniraMouseClickTracker;
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
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraClientSetupEvent>> apiModClientSetupCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraClientTickEvent>> apiClientTickCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraChatEvent>> apiClientChatCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraScreenOpenEvent>> apiScreenChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraTextureReloadEvent>> apiTextureReloadCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraDrawScreenEvent>> apiScreenPreRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraDrawScreenEvent>> apiScreenPostRenderCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraScreenEvent>> apiScreenCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraMouseEvent>> apiMouseClickedPreCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraMouseEvent>> apiMouseReleasedPreCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraMouseEvent>> apiMouseScrolledPreCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraKeyboardEvent>> apiKeyPressedPreCallbacks = new ArrayList<>();
    private static final List<Consumer<xin.vanilla.banira.api.client.event.BaniraKeyboardEvent>> apiKeyReleasedPostCallbacks = new ArrayList<>();

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
        fire(modClientSetupCallbacks, "mod client setup");
        fire(apiModClientSetupCallbacks, new xin.vanilla.banira.api.client.event.BaniraClientSetupEvent(), "api mod client setup");
    }

    public static void dispatchClientPlayerLoggedIn(PlayerEntity player) {
        fire(clientPlayerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchClientPlayerLoggedOut(PlayerEntity player) {
        fire(clientPlayerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchClientTick(BaniraClientTickEvent event) {
        fire(clientTickCallbacks, event, "client tick");
        if (event != null && event.phase() == BaniraTickPhase.END) {
            fire(apiClientTickCallbacks, xin.vanilla.banira.api.client.event.BaniraClientTickEvent.END, "api client tick");
        }
    }

    public static void dispatchClientChat(BaniraClientChatEvent event) {
        fire(clientChatCallbacks, event, "client chat");
        if (event != null) {
            fire(apiClientChatCallbacks, new xin.vanilla.banira.api.client.event.BaniraChatEvent(event.message()), "api client chat");
        }
    }

    public static void dispatchClientScreen(BaniraClientScreenEvent event) {
        fire(clientScreenCallbacks, event, "client screen");
        if (event != null) {
            fire(apiScreenCallbacks, new xin.vanilla.banira.api.client.event.BaniraScreenEvent(screenInfo(event.nativeScreen(Screen.class))), "api client screen");
        }
    }

    public static void dispatchClientScreenChanged(BaniraClientScreenEvent event) {
        fire(clientScreenChangedCallbacks, event, "client screen changed");
        resetInputTrackers();
        if (event != null) {
            fire(apiScreenChangedCallbacks, new xin.vanilla.banira.api.client.event.BaniraScreenOpenEvent(screenInfo(event.nativeScreen(Screen.class))), "api client screen changed");
        }
    }

    public static void dispatchClientScreenPreRender(BaniraClientScreenEvent event) {
        fire(clientScreenPreRenderCallbacks, event, "client screen pre render");
        if (event != null && event.draw() != null) {
            fire(apiScreenPreRenderCallbacks, drawScreenEvent(event), "api client screen pre render");
        }
    }

    public static void dispatchClientTextureReload(BaniraTextureReloadEvent event) {
        fire(clientTextureReloadCallbacks, event, "client texture reload");
        if (event != null && event.atlasLocation() != null) {
            fire(apiTextureReloadCallbacks, new xin.vanilla.banira.api.client.event.BaniraTextureReloadEvent(event.atlasLocation()), "api client texture reload");
        }
    }

    public static void dispatchClientScreenPostRender(BaniraClientScreenEvent event) {
        fire(clientScreenPostRenderCallbacks, event, "client screen post render");
        if (event != null && event.draw() != null) {
            fire(apiScreenPostRenderCallbacks, drawScreenEvent(event), "api client screen post render");
        }
    }

    public static void dispatchClientInput(BaniraClientInputEvent event) {
        fire(clientInputCallbacks, event, "client input");
        dispatchApiInputEvent(event);
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

        public static void onGuiChanged(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraScreenOpenEvent> callback) {
            apiScreenChangedCallbacks.add(callback);
        }

        public static void onScreenChanged(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenChangedCallbacks.add(callback);
        }

        public static void onTextureReload(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraTextureReloadEvent> callback) {
            apiTextureReloadCallbacks.add(callback);
        }

        public static void onDrawScreenPre(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraDrawScreenEvent> callback) {
            apiScreenPreRenderCallbacks.add(callback);
        }

        public static void onDrawScreenPost(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraDrawScreenEvent> callback) {
            apiScreenPostRenderCallbacks.add(callback);
        }

        public static void onScreenPreRender(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenPreRenderCallbacks.add(callback);
        }

        public static void onScreenPostRender(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenPostRenderCallbacks.add(callback);
        }

        public static void onClientTick(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraClientTickEvent> callback) {
            apiClientTickCallbacks.add(callback);
        }

        public static void onChat(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraChatEvent> callback) {
            apiClientChatCallbacks.add(callback);
        }

        public static void onGuiScreen(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraScreenEvent> callback) {
            apiScreenCallbacks.add(callback);
        }

        public static void onScreenEvent(@Nonnull Consumer<BaniraClientScreenEvent> callback) {
            clientScreenCallbacks.add(callback);
        }

        public static void onMouseClickedPre(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraMouseEvent> callback) {
            apiMouseClickedPreCallbacks.add(callback);
        }

        public static void onMouseReleasedPre(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraMouseEvent> callback) {
            apiMouseReleasedPreCallbacks.add(callback);
        }

        public static void onMouseScrolledPre(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraMouseEvent> callback) {
            apiMouseScrolledPreCallbacks.add(callback);
        }

        public static void onKeyPressedPre(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraKeyboardEvent> callback) {
            apiKeyPressedPreCallbacks.add(callback);
        }

        public static void onKeyReleasedPost(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraKeyboardEvent> callback) {
            apiKeyReleasedPostCallbacks.add(callback);
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

        public static void onClientSetup(@Nonnull Consumer<xin.vanilla.banira.api.client.event.BaniraClientSetupEvent> callback) {
            apiModClientSetupCallbacks.add(callback);
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

    public static xin.vanilla.banira.api.client.event.BaniraScreenInfo screenInfo(Screen screen) {
        if (screen == null) {
            return xin.vanilla.banira.api.client.event.BaniraScreenInfo.closed();
        }
        // 1.16.5 与高版本 Screen 的标题/尺寸命名不同，这里只做只读探测。
        return new xin.vanilla.banira.api.client.event.BaniraScreenInfo(
                screen.getClass().getName(),
                screenTitle(screen),
                screenDimension(screen, "width", "field_230708_k_", "f_96543_"),
                screenDimension(screen, "height", "field_230709_l_", "f_96544_"),
                true
        );
    }

    private static String screenTitle(Screen screen) {
        Object title = invokeNoArg(screen, "getTitle", "func_230705_a_");
        if (title == null) {
            title = fieldValue(screen, "title", "field_230704_d_", "f_96539_");
        }
        Object text = title == null ? null : invokeNoArg(title, "getString", "getContents", "getText");
        return text != null ? String.valueOf(text) : "";
    }

    private static int screenDimension(Screen screen, String... names) {
        Object value = fieldValue(screen, names);
        return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
    }

    private static Object invokeNoArg(Object target, String... methodNames) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            for (String methodName : methodNames) {
                try {
                    java.lang.reflect.Method method = type.getDeclaredMethod(methodName);
                    method.setAccessible(true);
                    return method.invoke(target);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Object fieldValue(Object target, String... fieldNames) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            for (String fieldName : fieldNames) {
                try {
                    java.lang.reflect.Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(target);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static void resetInputTrackers() {
        mouseClickTracker.reset();
        dragTracker.reset();
        keyPressTracker.reset();
    }

    private static xin.vanilla.banira.api.client.event.BaniraDrawScreenEvent drawScreenEvent(BaniraClientScreenEvent event) {
        return new xin.vanilla.banira.api.client.event.BaniraDrawScreenEvent(
                apiDrawContext(event.draw()),
                screenInfo(event.nativeScreen(Screen.class)),
                event.mouseX(),
                event.mouseY(),
                event.partialTicks()
        );
    }

    private static xin.vanilla.banira.api.client.render.BaniraDrawContext apiDrawContext(BaniraDrawContext legacyDraw) {
        return new xin.vanilla.banira.api.client.render.BaniraDrawContext(
                new BaniraLegacyDrawHandle(legacyDraw),
                legacyDraw != null ? legacyDraw.width() : 0,
                legacyDraw != null ? legacyDraw.height() : 0,
                legacyDraw != null ? legacyDraw.partialTicks() : 0.0F
        );
    }

    private static void dispatchApiInputEvent(BaniraClientInputEvent event) {
        if (event == null) {
            return;
        }
        Screen screen = event.nativeScreen(Screen.class);
        xin.vanilla.banira.api.client.event.BaniraScreenInfo screenInfo = screenInfo(screen);
        if (event.type() == BaniraClientInputEventType.MOUSE_CLICK) {
            dragTracker.press(event.mouseX(), event.mouseY(), event.button());
            xin.vanilla.banira.api.client.event.BaniraMouseEvent apiEvent =
                    xin.vanilla.banira.api.client.event.BaniraMouseEvent.clicked(screenInfo, event.mouseX(), event.mouseY(), event.button())
                            .withClickMetadata(mouseClickTracker.record(event.mouseX(), event.mouseY(), event.button()));
            fire(apiMouseClickedPreCallbacks, apiEvent, "api mouse clicked pre");
            if (apiEvent.canceled()) {
                event.cancel();
            }
            return;
        }
        if (event.type() == BaniraClientInputEventType.MOUSE_RELEASE) {
            xin.vanilla.banira.api.client.event.BaniraMouseEvent apiEvent =
                    xin.vanilla.banira.api.client.event.BaniraMouseEvent.released(screenInfo, event.mouseX(), event.mouseY(), event.button())
                            .withDragMetadata(dragTracker.release(event.mouseX(), event.mouseY(), event.button()));
            fire(apiMouseReleasedPreCallbacks, apiEvent, "api mouse released pre");
            if (apiEvent.canceled()) {
                event.cancel();
            }
            return;
        }
        if (event.type() == BaniraClientInputEventType.MOUSE_SCROLL) {
            xin.vanilla.banira.api.client.event.BaniraMouseEvent apiEvent =
                    xin.vanilla.banira.api.client.event.BaniraMouseEvent.scrolled(screenInfo, event.mouseX(), event.mouseY(), event.scrollDelta());
            fire(apiMouseScrolledPreCallbacks, apiEvent, "api mouse scrolled pre");
            if (apiEvent.canceled()) {
                event.cancel();
            }
            return;
        }
        if (event.type() == BaniraClientInputEventType.KEY_PRESS) {
            xin.vanilla.banira.api.client.event.BaniraKeyboardEvent apiEvent =
                    xin.vanilla.banira.api.client.event.BaniraKeyboardEvent.pressed(screenInfo, event.keyCode(), event.scanCode(), event.modifiers())
                            .withPressMetadata(keyPressTracker.recordPress(event.keyCode(), event.scanCode(), event.modifiers()));
            fire(apiKeyPressedPreCallbacks, apiEvent, "api key pressed pre");
            if (apiEvent.canceled()) {
                event.cancel();
            }
            return;
        }
        if (event.type() == BaniraClientInputEventType.KEY_RELEASE) {
            keyPressTracker.recordRelease(event.keyCode(), event.scanCode());
            xin.vanilla.banira.api.client.event.BaniraKeyboardEvent apiEvent =
                    xin.vanilla.banira.api.client.event.BaniraKeyboardEvent.released(screenInfo, event.keyCode(), event.scanCode(), event.modifiers());
            fire(apiKeyReleasedPostCallbacks, apiEvent, "api key released post");
            if (apiEvent.canceled()) {
                event.cancel();
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
                apiDrawContext(legacyDraw);
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
