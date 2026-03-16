package xin.vanilla.banira.common.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 事件总线工具类
 * <p>
 * 用于统一管理游戏事件的监听和回调，提供清晰的模块化 API。
 * </p>
 * <h3>使用示例</h3>
 * <pre>{@code
 * BaniraEventBus.Server.onStarting(server -> ...);
 * BaniraEventBus.Player.onLoggedOut(player -> ...);
 * BaniraEventBus.Client.onGuiChanged(() -> ...);
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

    private static final List<Consumer<PlayerEntity>> playerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEntity>> playerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEntity>> clientPlayerLoggedInCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEntity>> clientPlayerLoggedOutCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEvent.PlayerChangedDimensionEvent>> playerChangedDimensionCallbacks = new ArrayList<>();

    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Runnable> chunkSaveCallbacks = new ArrayList<>();
    private static final List<Consumer<ServerPlayerEntity>> playerSaveCallbacks = new ArrayList<>();

    private static final List<Consumer<GuiOpenEvent>> clientGuiChangedCallbacks = new ArrayList<>();
    private static final List<Consumer<TextureStitchEvent.Post>> clientTextureReloadCallbacks = new ArrayList<>();
    private static final List<Consumer<GuiScreenEvent.DrawScreenEvent.Post>> clientDrawScreenPostCallbacks = new ArrayList<>();
    private static final List<Consumer<RenderGameOverlayEvent.Post>> clientRenderOverlayPostCallbacks = new ArrayList<>();

    private static final List<Consumer<TickEvent.ServerTickEvent>> serverTickCallbacks = new ArrayList<>();

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
        public static void onTick(@Nonnull Consumer<TickEvent.ServerTickEvent> callback) {
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

        public static void onLoggedIn(@Nonnull Consumer<PlayerEntity> callback) {
            playerLoggedInCallbacks.add(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<PlayerEntity> callback) {
            playerLoggedOutCallbacks.add(callback);
        }

        public static void onClientLoggedIn(@Nonnull Consumer<PlayerEntity> callback) {
            clientPlayerLoggedInCallbacks.add(callback);
        }

        public static void onClientLoggedOut(@Nonnull Consumer<PlayerEntity> callback) {
            clientPlayerLoggedOutCallbacks.add(callback);
        }

        public static void onChangedDimension(@Nonnull Consumer<PlayerEvent.PlayerChangedDimensionEvent> callback) {
            playerChangedDimensionCallbacks.add(callback);
        }

        /**
         * 仅当玩家进入新维度时触发
         */
        public static void onEnterDimension(@Nonnull Consumer<ServerPlayerEntity> callback) {
            playerChangedDimensionCallbacks.add(event -> {
                if (event.getPlayer() instanceof ServerPlayerEntity) {
                    callback.accept((ServerPlayerEntity) event.getPlayer());
                }
            });
        }

        /**
         * 仅当玩家退出维度时触发
         */
        public static void onExitDimension(@Nonnull BiConsumer<ServerPlayerEntity, RegistryKey<World>> callback) {
            playerChangedDimensionCallbacks.add(event -> {
                if (event.getPlayer() instanceof ServerPlayerEntity) {
                    callback.accept((ServerPlayerEntity) event.getPlayer(), event.getFrom());
                }
            });
        }

        public static void onSave(@Nonnull Consumer<ServerPlayerEntity> callback) {
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

        public static void onChunkSave(@Nonnull Runnable callback) {
            chunkSaveCallbacks.add(callback);
        }

        public static void onPlayerSave(@Nonnull Consumer<ServerPlayerEntity> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：Client

    /**
     * 客户端相关事件（仅客户端可用）
     */
    public static final class Client {
        private Client() {
        }

        public static void onGuiChanged(@Nonnull Consumer<GuiOpenEvent> callback) {
            clientGuiChangedCallbacks.add(callback);
        }

        /**
         * 手动触发客户端界面变化事件（由 GameEventHandler 调用）
         */
        public static void fireGuiChanged(GuiOpenEvent event) {
            fire(clientGuiChangedCallbacks, event, "client gui changed");
        }

        @OnlyIn(Dist.CLIENT)
        public static void onTextureReload(@Nonnull Consumer<TextureStitchEvent.Post> callback) {
            clientTextureReloadCallbacks.add(callback);
        }

        @OnlyIn(Dist.CLIENT)
        public static void onDrawScreenPost(@Nonnull Consumer<GuiScreenEvent.DrawScreenEvent.Post> callback) {
            clientDrawScreenPostCallbacks.add(callback);
        }

        @OnlyIn(Dist.CLIENT)
        public static void onRenderOverlayPost(@Nonnull Consumer<RenderGameOverlayEvent.Post> callback) {
            clientRenderOverlayPostCallbacks.add(callback);
        }

        @OnlyIn(Dist.CLIENT)
        public static void fireTextureReload(TextureStitchEvent.Post event) {
            fire(clientTextureReloadCallbacks, event, "client texture reload");
        }

        @OnlyIn(Dist.CLIENT)
        public static void fireDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
            fire(clientDrawScreenPostCallbacks, event, "client draw screen post");
        }

        @OnlyIn(Dist.CLIENT)
        public static void fireRenderOverlayPost(RenderGameOverlayEvent.Post event) {
            fire(clientRenderOverlayPostCallbacks, event, "client render overlay post");
        }
    }

    // endregion

    // region Forge 事件订阅

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        fire(serverStartingCallbacks, event.getServer(), "server starting");
    }

    @SubscribeEvent
    public static void onServerStarted(FMLServerStartedEvent event) {
        fire(serverStartedCallbacks, event.getServer(), "server started");
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        fire(serverStoppingCallbacks, event.getServer(), "server stopping");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        fire(serverTickCallbacks, event, "server tick");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        fire(playerLoggedInCallbacks, event.getPlayer(), "player logged in");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        fire(clientPlayerLoggedInCallbacks, event.getPlayer(), "player logged in");
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        fire(playerLoggedOutCallbacks, event.getPlayer(), "player logged out");
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        fire(clientPlayerLoggedOutCallbacks, event.getPlayer(), "player logged out");
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        fire(playerChangedDimensionCallbacks, event, "player changed dimension");
    }

    @SubscribeEvent
    public static void onWorldSave(WorldEvent.Save event) {
        IWorld world = event.getWorld();
        if (!world.isClientSide()) {
            fire(worldSaveCallbacks, "world save");
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkEvent.Save event) {
        IWorld world = event.getWorld();
        if (world != null && !world.isClientSide()) {
            fire(chunkSaveCallbacks, "chunk save");
        }
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            fire(playerSaveCallbacks, (ServerPlayerEntity) player, "player save");
        }
    }

    // endregion

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
