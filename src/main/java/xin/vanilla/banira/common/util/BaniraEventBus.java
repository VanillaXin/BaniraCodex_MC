package xin.vanilla.banira.common.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 事件总线工具类
 * <p>
 * 用于统一管理游戏事件的监听和回调，提供清晰的模块化 API。
 * 客户端专用类型（{@code ScreenEvent} 等）见 {@link xin.vanilla.banira.client.event.BaniraClientEventHub}，不得写入本类，否则专用服无法 {@code register}。
 * </p>
 * <h3>使用示例</h3>
 * <pre>{@code
 * BaniraEventBus.Server.onStarting(server -> ...);
 * BaniraEventBus.Player.onLoggedOut(player -> ...);
 * BaniraClientEventHub.Client.onGuiChanged(e -> ...); // 客户端见 {@link xin.vanilla.banira.client.event.BaniraClientEventHub}
 * BaniraEventBus.WorldEvents.onUnload(e -> ...);
 * BaniraEventBus.EntityEvents.onJoinLevel(e -> ...);
 * BaniraEventBus.Commands.onRegister(e -> ...);
 * BaniraEventBus.ModLifecycle.onCommonSetup(e -> ...);
 *
 * // 支持取消注册
 * Registration reg = BaniraEventBus.registerServerStarting(server -> ...);
 * reg.unregister();  // 需要时取消
 * }</pre>
 */
public final class BaniraEventBus {
    private BaniraEventBus() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    // region 回调存储

    private static final List<Consumer<MinecraftServer>> serverStartingCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStartedCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStoppingCallbacks = new ArrayList<>();

    private static final List<Consumer<net.minecraft.world.entity.player.Player>> playerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<net.minecraft.world.entity.player.Player>> playerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Consumer<ServerPlayer>> playerSaveCallbacks = new ArrayList<>();

    private static final List<Consumer<MinecraftServer>> serverTickCallbacks = new ArrayList<>();

    // endregion

    // region 公共 API：Registration

    /**
     * 事件注册句柄，用于取消已注册的回调
     */
    public interface Registration {
        void unregister();
    }

    private static Registration createRegistration(Runnable unregister) {
        return unregister::run;
    }

    // endregion

    // region 分类 API：Server

    /**
     * 服务器相关事件
     */
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

        public static Registration onStartingWithRegistration(@Nonnull Consumer<MinecraftServer> callback) {
            serverStartingCallbacks.add(callback);
            return createRegistration(() -> serverStartingCallbacks.remove(callback));
        }

        public static Registration onStoppingWithRegistration(@Nonnull Consumer<MinecraftServer> callback) {
            serverStoppingCallbacks.add(callback);
            return createRegistration(() -> serverStoppingCallbacks.remove(callback));
        }

        /**
         * 注册服务器每 tick 回调（Phase.END 阶段）
         */
        public static void onTick(@Nonnull Consumer<MinecraftServer> callback) {
            serverTickCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：Player

    /**
     * 玩家相关事件
     */
    public static final class Player {
        private Player() {
        }

        public static void onLoggedIn(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            playerLoggedInCallbacks.add(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            playerLoggedOutCallbacks.add(callback);
        }

        public static void onSave(@Nonnull Consumer<ServerPlayer> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：Save

    /**
     * 保存相关事件
     */
    public static final class Save {
        private Save() {
        }

        public static void onWorldSave(@Nonnull Runnable callback) {
            worldSaveCallbacks.add(callback);
        }

        public static void onPlayerSave(@Nonnull Consumer<ServerPlayer> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    // endregion

    // region Fabric 事件转发

    public static void dispatchServerStarting(MinecraftServer server) {
        fire(serverStartingCallbacks, server, "server starting");
    }

    public static void dispatchServerStarted(MinecraftServer server) {
        fire(serverStartedCallbacks, server, "server started");
    }

    public static void dispatchServerStopping(MinecraftServer server) {
        fire(serverStoppingCallbacks, server, "server stopping");
        fire(worldSaveCallbacks, "world save");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            fire(playerSaveCallbacks, player, "player save");
        }
    }

    public static void dispatchServerTick(MinecraftServer server) {
        fire(serverTickCallbacks, server, "server tick");
    }

    public static void dispatchPlayerLoggedIn(ServerPlayer player) {
        fire(playerLoggedInCallbacks, player, "player logged in");
    }

    public static void dispatchPlayerLoggedOut(ServerPlayer player) {
        fire(playerLoggedOutCallbacks, player, "player logged out");
        fire(playerSaveCallbacks, player, "player save");
    }

    // endregion Fabric 事件转发

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

    private static void fire(List<Runnable> callbacks, String eventName) {
        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for {} event", eventName, t);
            }
        }
    }

    // endregion

}
