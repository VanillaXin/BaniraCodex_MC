package xin.vanilla.banira.common.util;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
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

    private static final List<Consumer<PlayerEntity>> playerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEntity>> playerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<ServerPlayerEntity>> playerEnteredDimensionCallbacks = new ArrayList<>();
    private static final List<BiConsumer<ServerPlayerEntity, RegistryKey<World>>> playerExitedDimensionCallbacks = new ArrayList<>();
    private static final List<Consumer<ServerPlayerEntity>> playerSaveCallbacks = new ArrayList<>();

    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Runnable> chunkSaveCallbacks = new ArrayList<>();
    private static final List<Consumer<CommandDispatcher<CommandSource>>> commandDispatcherCallbacks = new ArrayList<>();
    private static final List<Runnable> modCommonSetupRunnables = new ArrayList<>();

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

    public static final class Player {
        private Player() {
        }

        public static void onLoggedIn(@Nonnull Consumer<PlayerEntity> callback) {
            playerLoggedInCallbacks.add(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<PlayerEntity> callback) {
            playerLoggedOutCallbacks.add(callback);
        }

        public static void onEnterDimension(@Nonnull Consumer<ServerPlayerEntity> callback) {
            playerEnteredDimensionCallbacks.add(callback);
        }

        public static void onExitDimension(@Nonnull BiConsumer<ServerPlayerEntity, RegistryKey<World>> callback) {
            playerExitedDimensionCallbacks.add(callback);
        }

        public static void onSave(@Nonnull Consumer<ServerPlayerEntity> callback) {
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

        public static void onPlayerSave(@Nonnull Consumer<ServerPlayerEntity> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    public static final class Commands {
        private Commands() {
        }

        public static void onRegisterDispatcher(@Nonnull Consumer<CommandDispatcher<CommandSource>> callback) {
            commandDispatcherCallbacks.add(callback);
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onCommonSetup(@Nonnull Runnable callback) {
            modCommonSetupRunnables.add(callback);
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

    public static void dispatchPlayerLoggedIn(PlayerEntity player) {
        fire(playerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchPlayerLoggedOut(PlayerEntity player) {
        fire(playerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchPlayerChangedDimension(ServerPlayerEntity player, RegistryKey<World> from) {
        fire(playerEnteredDimensionCallbacks, player, "player enter dimension");
        fire(playerExitedDimensionCallbacks, player, from, "player exit dimension");
    }

    public static void dispatchWorldSave() {
        fire(worldSaveCallbacks, "world save");
    }

    public static void dispatchChunkSave() {
        fire(chunkSaveCallbacks, "chunk save");
    }

    public static void dispatchPlayerSave(ServerPlayerEntity player) {
        fire(playerSaveCallbacks, player, "player save");
    }

    public static void dispatchCommandRegistration(CommandDispatcher<CommandSource> dispatcher) {
        fire(commandDispatcherCallbacks, dispatcher, "register command dispatcher");
    }

    public static void dispatchModCommonSetup() {
        fire(modCommonSetupRunnables, "mod common setup");
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

    private static <A, B> void fire(List<BiConsumer<A, B>> callbacks, A first, B second, String eventName) {
        for (BiConsumer<A, B> callback : callbacks) {
            try {
                callback.accept(first, second);
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
