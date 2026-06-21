package xin.vanilla.banira.common.util;

import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraLifecycle;
import xin.vanilla.banira.api.event.BaniraPlayerDimensionEvent;
import xin.vanilla.banira.api.event.BaniraPlayerEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Loader-neutral event hub. Loader adapters dispatch platform events into this class.
 */
public final class BaniraEventBus {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<MinecraftServer>> serverStartingCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStartedCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStoppingCallbacks = new ArrayList<>();
    private static final List<Runnable> serverTickEndCallbacks = new ArrayList<>();
    private static final List<Runnable> clientTickEndCallbacks = new ArrayList<>();

    private static final List<Consumer<BaniraPlayerEvent>> playerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraPlayerEvent>> playerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraPlayerDimensionEvent>> playerChangedDimensionCallbacks = new ArrayList<>();
    private static final List<Consumer<BaniraPlayerEvent>> playerSaveCallbacks = new ArrayList<>();

    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Runnable> chunkSaveCallbacks = new ArrayList<>();

    private BaniraEventBus() {
    }

    public interface Registration {
        void unregister();
    }

    private static Registration createRegistration(Runnable unregister) {
        return unregister::run;
    }

    public static final class Server {
        private Server() {
        }

        public static void onStarting(@Nonnull Consumer<MinecraftServer> callback) {
            serverStartingCallbacks.add(callback);
        }

        public static void onStarted(@Nonnull Consumer<MinecraftServer> callback) {
            serverStartedCallbacks.add(callback);
        }

        public static void onStopping(@Nonnull Consumer<MinecraftServer> callback) {
            serverStoppingCallbacks.add(callback);
        }

        public static void onTickEnd(@Nonnull Runnable callback) {
            serverTickEndCallbacks.add(callback);
        }

        public static Registration onStartingWithRegistration(@Nonnull Consumer<MinecraftServer> callback) {
            serverStartingCallbacks.add(callback);
            return createRegistration(() -> serverStartingCallbacks.remove(callback));
        }

        public static Registration onStoppingWithRegistration(@Nonnull Consumer<MinecraftServer> callback) {
            serverStoppingCallbacks.add(callback);
            return createRegistration(() -> serverStoppingCallbacks.remove(callback));
        }
    }

    public static final class Client {
        private Client() {
        }

        public static void onTickEnd(@Nonnull Runnable callback) {
            clientTickEndCallbacks.add(callback);
        }
    }

    public static final class Player {
        private Player() {
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

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onCommonSetup(@Nonnull Runnable callback) {
            BaniraLifecycle.onCommonSetup(event -> event.enqueueWork(callback));
        }
    }

    public static void dispatchServerStarting(MinecraftServer server) {
        fire(serverStartingCallbacks, server, "server starting");
    }

    public static void dispatchServerStarted(MinecraftServer server) {
        fire(serverStartedCallbacks, server, "server started");
    }

    public static void dispatchServerStopping(MinecraftServer server) {
        fire(serverStoppingCallbacks, server, "server stopping");
    }

    public static void dispatchServerTickEnd() {
        fire(serverTickEndCallbacks, "server tick end");
    }

    public static void dispatchClientTickEnd() {
        fire(clientTickEndCallbacks, "client tick end");
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

    public static void dispatchModCommonSetup() {
        dispatchModCommonSetup(BaniraCommonSetupEvent.immediate());
    }

    public static void dispatchModCommonSetup(@Nonnull BaniraCommonSetupEvent event) {
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
