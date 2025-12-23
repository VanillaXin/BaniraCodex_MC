package xin.vanilla.banira.common.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.IWorld;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 事件总线工具类</br>
 * 用于统一管理游戏事件的监听和回调
 */
public final class BaniraEventBus {
    private BaniraEventBus() {
    }

    private static final Logger LOGGER = LogManager.getLogger();

    // 服务器事件回调列表
    private static final List<Consumer<MinecraftServer>> serverStartingCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStartedCallbacks = new ArrayList<>();
    private static final List<Consumer<MinecraftServer>> serverStoppingCallbacks = new ArrayList<>();

    // 玩家事件回调列表
    private static final List<Consumer<PlayerEntity>> playerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEntity>> playerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEvent.PlayerChangedDimensionEvent>> playerChangedDimensionCallbacks = new ArrayList<>();

    // 保存事件回调列表
    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Runnable> chunkSaveCallbacks = new ArrayList<>();
    private static final List<Consumer<ServerPlayerEntity>> playerSaveCallbacks = new ArrayList<>();

    // 客户端事件回调列表（例如界面变化、纹理重载）
    private static final List<Runnable> clientGuiChangedCallbacks = new ArrayList<>();
    private static final List<Runnable> clientTextureReloadCallbacks = new ArrayList<>();


    // region 服务器事件注册

    /**
     * 注册服务器启动时回调（FMLServerStartingEvent）
     */
    public static void registerServerStarting(@Nonnull Consumer<MinecraftServer> callback) {
        serverStartingCallbacks.add(callback);
    }

    /**
     * 注册服务器已启动时回调（FMLServerStartedEvent）
     */
    public static void registerServerStarted(@Nonnull Consumer<MinecraftServer> callback) {
        serverStartedCallbacks.add(callback);
    }

    /**
     * 注册服务器关闭时回调（FMLServerStoppingEvent）
     */
    public static void registerServerStopping(@Nonnull Consumer<MinecraftServer> callback) {
        serverStoppingCallbacks.add(callback);
    }

    // endregion

    // region 玩家事件注册

    /**
     * 注册玩家进入服务器时回调（PlayerEvent.PlayerLoggedInEvent）
     */
    public static void registerPlayerLoggedIn(@Nonnull Consumer<PlayerEntity> callback) {
        playerLoggedInCallbacks.add(callback);
    }

    /**
     * 注册玩家退出服务器时回调（PlayerEvent.PlayerLoggedOutEvent）
     */
    public static void registerPlayerLoggedOut(@Nonnull Consumer<PlayerEntity> callback) {
        playerLoggedOutCallbacks.add(callback);
    }

    /**
     * 注册玩家维度变化时回调（PlayerEvent.PlayerChangedDimensionEvent）
     * 包括进入和退出维度
     *
     */
    public static void registerPlayerChangedDimension(@Nonnull Consumer<PlayerEvent.PlayerChangedDimensionEvent> callback) {
        playerChangedDimensionCallbacks.add(callback);
    }

    /**
     * 注册玩家进入维度时回调（PlayerEvent.PlayerChangedDimensionEvent）
     * 仅当玩家进入新维度时触发
     */
    public static void registerPlayerEnterDimension(@Nonnull Consumer<ServerPlayerEntity> callback) {
        playerChangedDimensionCallbacks.add(event -> {
            if (event.getPlayer() instanceof ServerPlayerEntity) {
                callback.accept((ServerPlayerEntity) event.getPlayer());
            }
        });
    }

    /**
     * 注册玩家退出维度时回调（PlayerEvent.PlayerChangedDimensionEvent）
     * 仅当玩家退出维度时触发
     */
    public static void registerPlayerExitDimension(@Nonnull java.util.function.BiConsumer<ServerPlayerEntity, net.minecraft.util.RegistryKey<net.minecraft.world.World>> callback) {
        playerChangedDimensionCallbacks.add(event -> {
            if (event.getPlayer() instanceof ServerPlayerEntity) {
                callback.accept((ServerPlayerEntity) event.getPlayer(), event.getFrom());
            }
        });
    }

    // endregion

    // region 保存事件注册

    /**
     * 注册世界保存时回调（WorldEvent.Save）
     */
    public static void registerWorldSave(@Nonnull Runnable callback) {
        worldSaveCallbacks.add(callback);
    }

    /**
     * 注册区块保存时回调（ChunkEvent.Save）
     */
    public static void registerChunkSave(@Nonnull Runnable callback) {
        chunkSaveCallbacks.add(callback);
    }

    /**
     * 注册玩家数据保存时回调（PlayerEvent.SaveToFile）
     */
    public static void registerPlayerSave(@Nonnull Consumer<ServerPlayerEntity> callback) {
        playerSaveCallbacks.add(callback);
    }

    /**
     * 注册客户端界面变化时回调（由客户端事件处理器触发）
     */
    public static void registerClientGuiChanged(@Nonnull Runnable callback) {
        clientGuiChangedCallbacks.add(callback);
    }

    /**
     * 注册客户端纹理重载后回调（由客户端事件处理器触发）
     */
    public static void registerClientTextureReload(@Nonnull Runnable callback) {
        clientTextureReloadCallbacks.add(callback);
    }

    // endregion

    // region 事件处理器

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        executeCallbacks(serverStartingCallbacks, server, "server starting");
    }

    @SubscribeEvent
    public static void onServerStarted(FMLServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        executeCallbacks(serverStartedCallbacks, server, "server started");
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        executeCallbacks(serverStoppingCallbacks, server, "server stopping");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerEntity player = event.getPlayer();
        executeCallbacks(playerLoggedInCallbacks, player, "player logged in");
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEntity player = event.getPlayer();
        executeCallbacks(playerLoggedOutCallbacks, player, "player logged out");
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        executeCallbacks(playerChangedDimensionCallbacks, event, "player changed dimension");
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        IWorld world = event.getWorld();
        // 只在服务端世界保存时触发
        if (!world.isClientSide()) {
            executeRunnableCallbacks(worldSaveCallbacks, "world save");
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkEvent.Save event) {
        IWorld world = event.getWorld();
        // 只在服务端区块保存时触发
        if (world != null && !world.isClientSide()) {
            executeRunnableCallbacks(chunkSaveCallbacks, "chunk save");
        }
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            executeCallbacks(playerSaveCallbacks, (ServerPlayerEntity) player, "player save");
        }
    }

    /**
     * 客户端界面变化
     */
    public static void fireClientGuiChanged() {
        executeRunnableCallbacks(clientGuiChangedCallbacks, "client gui changed");
    }

    /**
     * 客户端纹理重载
     */
    public static void fireClientTextureReload() {
        executeRunnableCallbacks(clientTextureReloadCallbacks, "client texture reload");
    }

    // endregion

    // region 工具方法

    /**
     * 执行回调函数列表
     */
    private static <T> void executeCallbacks(List<Consumer<T>> callbacks, T parameter, String eventName) {
        for (Consumer<T> callback : callbacks) {
            try {
                callback.accept(parameter);
            } catch (Throwable t) {
                LOGGER.warn("Error executing callback for {} event", eventName, t);
            }
        }
    }

    /**
     * 执行无参数回调函数列表
     */
    private static void executeRunnableCallbacks(List<Runnable> callbacks, String eventName) {
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
