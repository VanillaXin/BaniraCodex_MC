package xin.vanilla.banira.common.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraEventRegistration;
import xin.vanilla.banira.api.event.BaniraLifecycle;
import xin.vanilla.banira.api.event.BaniraPlayerDimensionEvent;
import xin.vanilla.banira.api.event.BaniraPlayerEvent;
import xin.vanilla.banira.api.event.BaniraServerEvent;
import xin.vanilla.banira.api.event.BaniraWorldEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 加载器无关的事件回调中心；Forge/Fabric/NeoForge 事件只在 internal adapter 中转换。
 */
public final class BaniraEventBus {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<BaniraServerEvent>> serverStartingCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraServerEvent>> serverStartedCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraServerEvent>> serverStoppingCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraServerEvent>> serverTickCallbacks = new ArrayList<>();

    private static final List<Consumer<BaniraPlayerEvent>> playerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraPlayerEvent>> playerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraPlayerDimensionEvent>> playerChangedDimensionCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraPlayerEvent>> playerSaveCallbacks = new ArrayList<>();

    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Runnable> chunkSaveCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraWorldEvent>> worldUnloadCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraWorldEvent>> worldTickCallbacks = new ArrayList<>();

    private BaniraEventBus() {
    }

    /**
     * @deprecated 使用 {@link BaniraEventRegistration}。
     */
    @Deprecated
    public interface Registration {
        void unregister();
    }

    private static Registration createRegistration(Runnable unregister) {
        return unregister::run;
    }

    public static final class Server {
        private Server() {
        }

        public static void onStarting(@Nonnull Consumer<BaniraServerEvent> callback) {
            serverStartingCallbacks.add(callback);
        }

        public static void onStarted(@Nonnull Consumer<BaniraServerEvent> callback) {
            serverStartedCallbacks.add(callback);
        }

        public static void onStopping(@Nonnull Consumer<BaniraServerEvent> callback) {
            serverStoppingCallbacks.add(callback);
        }

        /**
         * 服务器 tick 回调；各加载器 adapter 只在 END 阶段派发。
         */
        public static void onTick(@Nonnull Consumer<BaniraServerEvent> callback) {
            serverTickCallbacks.add(callback);
        }

        public static Registration onStartingWithRegistration(@Nonnull Consumer<BaniraServerEvent> callback) {
            serverStartingCallbacks.add(callback);
            return createRegistration(() -> serverStartingCallbacks.remove(callback));
        }

        public static Registration onStoppingWithRegistration(@Nonnull Consumer<BaniraServerEvent> callback) {
            serverStoppingCallbacks.add(callback);
            return createRegistration(() -> serverStoppingCallbacks.remove(callback));
        }
    }

    public static final class PlayerEvents {
        private PlayerEvents() {
        }

        public static void onLoggedIn(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            playerLoggedInCallbacks.add(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            playerLoggedOutCallbacks.add(callback);
        }

        public static void onChangedDimension(@Nonnull Consumer<BaniraPlayerDimensionEvent> callback) {
            playerChangedDimensionCallbacks.add(callback);
        }

        public static void onSave(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    /**
     * 旧名称保留为分类别名，避免 Banira 自身调用处频繁变动。
     */
    public static final class Player {
        private Player() {
        }

        public static void onLoggedIn(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            PlayerEvents.onLoggedIn(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            PlayerEvents.onLoggedOut(callback);
        }

        public static void onChangedDimension(@Nonnull Consumer<BaniraPlayerDimensionEvent> callback) {
            PlayerEvents.onChangedDimension(callback);
        }

        public static void onSave(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            PlayerEvents.onSave(callback);
        }
    }

    public static final class Save {
        private Save() {
        }

        public static void onWorldSave(@Nonnull Runnable callback) {
            worldSaveCallbacks.add(callback);
        }

        public static void onChunkSave(@Nonnull Runnable callback) {
            chunkSaveCallbacks.add(callback);
        }

        public static void onPlayerSave(@Nonnull Consumer<BaniraPlayerEvent> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    public static final class WorldEvents {
        private WorldEvents() {
        }

        public static void onUnload(@Nonnull Consumer<BaniraWorldEvent> callback) {
            worldUnloadCallbacks.add(callback);
        }

        public static void onTick(@Nonnull Consumer<BaniraWorldEvent> callback) {
            worldTickCallbacks.add(callback);
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static Registration onCommonSetup(@Nonnull Consumer<BaniraCommonSetupEvent> callback) {
            BaniraEventRegistration registration = BaniraLifecycle.onCommonSetup(callback);
            return registration::unregister;
        }
    }

    public static void dispatchServerStarting(@Nonnull BaniraServerEvent event) {
        fire(serverStartingCallbacks, event, "server starting");
    }

    public static void dispatchServerStarted(@Nonnull BaniraServerEvent event) {
        fire(serverStartedCallbacks, event, "server started");
    }

    public static void dispatchServerStopping(@Nonnull BaniraServerEvent event) {
        fire(serverStoppingCallbacks, event, "server stopping");
    }

    public static void dispatchServerTick(@Nonnull BaniraServerEvent event) {
        fire(serverTickCallbacks, event, "server tick");
    }

    public static void dispatchPlayerLoggedIn(@Nonnull BaniraPlayerEvent event) {
        fire(playerLoggedInCallbacks, event, "player logged in");
    }

    public static void dispatchPlayerLoggedOut(@Nonnull BaniraPlayerEvent event) {
        fire(playerLoggedOutCallbacks, event, "player logged out");
    }

    public static void dispatchPlayerChangedDimension(@Nonnull BaniraPlayerDimensionEvent event) {
        fire(playerChangedDimensionCallbacks, event, "player changed dimension");
    }

    public static void dispatchWorldSave() {
        fire(worldSaveCallbacks, "world save");
    }

    public static void dispatchChunkSave() {
        fire(chunkSaveCallbacks, "chunk save");
    }

    public static void dispatchPlayerSave(@Nonnull BaniraPlayerEvent event) {
        fire(playerSaveCallbacks, event, "player save");
    }

    public static void dispatchWorldUnload(@Nonnull BaniraWorldEvent event) {
        fire(worldUnloadCallbacks, event, "world unload");
    }

    public static void dispatchWorldTick(@Nonnull BaniraWorldEvent event) {
        fire(worldTickCallbacks, event, "world tick");
    }

    public static void dispatchCommonSetup(@Nonnull BaniraCommonSetupEvent event) {
        BaniraLifecycle.dispatchCommonSetup(event);
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
}
