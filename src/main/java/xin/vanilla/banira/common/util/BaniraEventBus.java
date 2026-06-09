package xin.vanilla.banira.common.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 加载器无关的服务端事件回调中心；Forge/Fabric/NeoForge 事件只在 internal adapter 中转换。
 */
public final class BaniraEventBus {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<Consumer<MinecraftServer>> serverStartingCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStartedCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStoppingCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverTickCallbacks = new ArrayList<>();

    private static final List<Consumer<net.minecraft.world.entity.player.Player>> playerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<net.minecraft.world.entity.player.Player>> playerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerChangedDimensionEvent>> playerChangedDimensionCallbacks = new ArrayList<>();
    private static final List<Consumer<ServerPlayer>> playerSaveCallbacks = new ArrayList<>();

    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Runnable> chunkSaveCallbacks = new ArrayList<>();
    private static final List<Consumer<LevelAccessor>> worldUnloadCallbacks = new ArrayList<>();
    private static final List<Consumer<LevelAccessor>> worldTickCallbacks = new ArrayList<>();

    private static final List<Consumer<BaniraCommonSetupEvent>> modCommonSetupCallbacks = new ArrayList<>();

    private BaniraEventBus() {
    }

    public interface Registration {
        void unregister();
    }

    public record PlayerChangedDimensionEvent(ServerPlayer player, ResourceKey<Level> from, ResourceKey<Level> to) {
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

        /**
         * 服务器 tick 回调；各加载器 adapter 只在 END 阶段派发。
         */
        public static void onTick(@Nonnull Consumer<MinecraftServer> callback) {
            serverTickCallbacks.add(callback);
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

    public static final class PlayerEvents {
        private PlayerEvents() {
        }

        public static void onLoggedIn(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            playerLoggedInCallbacks.add(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            playerLoggedOutCallbacks.add(callback);
        }

        public static void onChangedDimension(@Nonnull Consumer<PlayerChangedDimensionEvent> callback) {
            playerChangedDimensionCallbacks.add(callback);
        }

        public static void onEnterDimension(@Nonnull Consumer<ServerPlayer> callback) {
            playerChangedDimensionCallbacks.add(event -> callback.accept(event.player()));
        }

        public static void onExitDimension(@Nonnull BiConsumer<ServerPlayer, ResourceKey<Level>> callback) {
            playerChangedDimensionCallbacks.add(event -> callback.accept(event.player(), event.from()));
        }

        public static void onSave(@Nonnull Consumer<ServerPlayer> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    /**
     * 旧名称保留为分类别名，避免 Banira 自身调用处频繁变动。
     */
    public static final class Player {
        private Player() {
        }

        public static void onLoggedIn(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            PlayerEvents.onLoggedIn(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            PlayerEvents.onLoggedOut(callback);
        }

        public static void onChangedDimension(@Nonnull Consumer<PlayerChangedDimensionEvent> callback) {
            PlayerEvents.onChangedDimension(callback);
        }

        public static void onEnterDimension(@Nonnull Consumer<ServerPlayer> callback) {
            PlayerEvents.onEnterDimension(callback);
        }

        public static void onExitDimension(@Nonnull BiConsumer<ServerPlayer, ResourceKey<Level>> callback) {
            PlayerEvents.onExitDimension(callback);
        }

        public static void onSave(@Nonnull Consumer<ServerPlayer> callback) {
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

        public static void onPlayerSave(@Nonnull Consumer<ServerPlayer> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    public static final class WorldEvents {
        private WorldEvents() {
        }

        public static void onUnload(@Nonnull Consumer<LevelAccessor> callback) {
            worldUnloadCallbacks.add(callback);
        }

        public static void onTick(@Nonnull Consumer<LevelAccessor> callback) {
            worldTickCallbacks.add(callback);
        }
    }

    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static Registration onCommonSetup(@Nonnull Consumer<BaniraCommonSetupEvent> callback) {
            modCommonSetupCallbacks.add(callback);
            return createRegistration(() -> modCommonSetupCallbacks.remove(callback));
        }
    }

    public static void dispatchServerStarting(@Nonnull MinecraftServer server) {
        fire(serverStartingCallbacks, server, "server starting");
    }

    public static void dispatchServerStarted(@Nonnull MinecraftServer server) {
        fire(serverStartedCallbacks, server, "server started");
    }

    public static void dispatchServerStopping(@Nonnull MinecraftServer server) {
        fire(serverStoppingCallbacks, server, "server stopping");
    }

    public static void dispatchServerTick(@Nonnull MinecraftServer server) {
        fire(serverTickCallbacks, server, "server tick");
    }

    public static void dispatchPlayerLoggedIn(@Nonnull net.minecraft.world.entity.player.Player player) {
        fire(playerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchPlayerLoggedOut(@Nonnull net.minecraft.world.entity.player.Player player) {
        fire(playerLoggedOutCallbacks, player, "player logged out");
    }

    public static void dispatchPlayerChangedDimension(@Nonnull ServerPlayer player,
                                                      @Nonnull ResourceKey<Level> from,
                                                      @Nonnull ResourceKey<Level> to) {
        fire(playerChangedDimensionCallbacks, new PlayerChangedDimensionEvent(player, from, to), "player changed dimension");
    }

    public static void dispatchWorldSave() {
        fire(worldSaveCallbacks, "world save");
    }

    public static void dispatchChunkSave() {
        fire(chunkSaveCallbacks, "chunk save");
    }

    public static void dispatchPlayerSave(@Nonnull ServerPlayer player) {
        fire(playerSaveCallbacks, player, "player save");
    }

    public static void dispatchWorldUnload(@Nonnull LevelAccessor world) {
        fire(worldUnloadCallbacks, world, "world unload");
    }

    public static void dispatchWorldTick(@Nonnull LevelAccessor world) {
        fire(worldTickCallbacks, world, "world tick");
    }

    public static void dispatchCommonSetup(@Nonnull BaniraCommonSetupEvent event) {
        fire(modCommonSetupCallbacks, event, "mod common setup");
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
