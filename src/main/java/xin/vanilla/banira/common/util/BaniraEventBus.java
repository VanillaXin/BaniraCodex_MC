package xin.vanilla.banira.common.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
    private static final List<Consumer<PlayerEvent.PlayerChangedDimensionEvent>> playerChangedDimensionCallbacks = new ArrayList<>();

    private static final List<Runnable> worldSaveCallbacks = new ArrayList<>();
    private static final List<Runnable> chunkSaveCallbacks = new ArrayList<>();
    private static final List<Consumer<ServerPlayer>> playerSaveCallbacks = new ArrayList<>();

    private static final List<Consumer<TickEvent.ServerTickEvent>> serverTickCallbacks = new ArrayList<>();
    private static final List<Consumer<TickEvent.LevelTickEvent>> worldTickCallbacks = new ArrayList<>();

    private static final List<Consumer<LevelEvent.Unload>> worldUnloadCallbacks = new ArrayList<>();

    private static final List<Consumer<PlayerEvent.Clone>> playerCloneCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerEvent>> playerEventCallbacks = new ArrayList<>();

    private static final List<Consumer<PlayerInteractEvent.RightClickItem>> playerRightClickItemCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerInteractEvent.RightClickBlock>> playerRightClickBlockCallbacks = new ArrayList<>();
    private static final List<Consumer<PlayerInteractEvent.EntityInteractSpecific>> playerEntityInteractCallbacks = new ArrayList<>();

    private static final List<Consumer<EntityJoinLevelEvent>> entityJoinLevelCallbacks = new ArrayList<>();
    private static final List<Consumer<EntityTeleportEvent>> entityTeleportCallbacks = new ArrayList<>();

    private static final List<Consumer<RegisterCommandsEvent>> registerCommandsCallbacks = new ArrayList<>();
    private static final List<Consumer<FMLCommonSetupEvent>> modCommonSetupCallbacks = new ArrayList<>();

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

        public static void onLoggedIn(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            playerLoggedInCallbacks.add(callback);
        }

        public static void onLoggedOut(@Nonnull Consumer<net.minecraft.world.entity.player.Player> callback) {
            playerLoggedOutCallbacks.add(callback);
        }

        public static void onChangedDimension(@Nonnull Consumer<PlayerEvent.PlayerChangedDimensionEvent> callback) {
            playerChangedDimensionCallbacks.add(callback);
        }

        /**
         * 仅当玩家进入新维度时触发
         */
        public static void onEnterDimension(@Nonnull Consumer<ServerPlayer> callback) {
            playerChangedDimensionCallbacks.add(event -> {
                if (event.getEntity() instanceof ServerPlayer player) {
                    callback.accept(player);
                }
            });
        }

        /**
         * 仅当玩家退出维度时触发
         */
        public static void onExitDimension(@Nonnull BiConsumer<ServerPlayer, ResourceKey<Level>> callback) {
            playerChangedDimensionCallbacks.add(event -> {
                if (event.getEntity() instanceof ServerPlayer player) {
                    callback.accept(player, event.getFrom());
                }
            });
        }

        public static void onSave(@Nonnull Consumer<ServerPlayer> callback) {
            playerSaveCallbacks.add(callback);
        }

        public static void onClone(@Nonnull Consumer<PlayerEvent.Clone> callback) {
            playerCloneCallbacks.add(callback);
        }

        /**
         * 任意 {@link PlayerEvent}（含登录、克隆等所有子类；若只需克隆请用 {@link #onClone}）
         */
        public static void onPlayerEvent(@Nonnull Consumer<PlayerEvent> callback) {
            playerEventCallbacks.add(callback);
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

        public static void onPlayerSave(@Nonnull Consumer<ServerPlayer> callback) {
            playerSaveCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：WorldEvents

    /**
     * 世界加载/卸载与世界级 Tick（与 {@link Level} 区分命名）
     */
    public static final class WorldEvents {
        private WorldEvents() {
        }

        public static void onUnload(@Nonnull Consumer<LevelEvent.Unload> callback) {
            worldUnloadCallbacks.add(callback);
        }

        public static void onTick(@Nonnull Consumer<TickEvent.LevelTickEvent> callback) {
            worldTickCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：EntityEvents

    /**
     * 实体进入世界、传送等
     */
    public static final class EntityEvents {
        private EntityEvents() {
        }

        @Deprecated
        public static void onJoinWorld(@Nonnull Consumer<EntityJoinLevelEvent> callback) {
            entityJoinLevelCallbacks.add(callback);
        }

        public static void onJoinLevel(@Nonnull Consumer<EntityJoinLevelEvent> callback) {
            entityJoinLevelCallbacks.add(callback);
        }

        public static void onTeleport(@Nonnull Consumer<EntityTeleportEvent> callback) {
            entityTeleportCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：Interaction

    /**
     * 玩家交互（右键物品/方块/实体）
     */
    public static final class Interaction {
        private Interaction() {
        }

        public static void onRightClickItem(@Nonnull Consumer<PlayerInteractEvent.RightClickItem> callback) {
            playerRightClickItemCallbacks.add(callback);
        }

        public static void onRightClickBlock(@Nonnull Consumer<PlayerInteractEvent.RightClickBlock> callback) {
            playerRightClickBlockCallbacks.add(callback);
        }

        public static void onEntityInteractSpecific(@Nonnull Consumer<PlayerInteractEvent.EntityInteractSpecific> callback) {
            playerEntityInteractCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：Commands

    /**
     * 指令注册（Forge 游戏总线 {@link RegisterCommandsEvent}）
     */
    public static final class Commands {
        private Commands() {
        }

        public static void onRegister(@Nonnull Consumer<RegisterCommandsEvent> callback) {
            registerCommandsCallbacks.add(callback);
        }
    }

    // endregion

    // region 分类 API：ModLifecycle

    /**
     * Mod 公共加载阶段（由 {@link xin.vanilla.banira.BaniraCodex} 对 Mod 总线 {@code addListener}）；客户端 {@link net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent} 见 {@link xin.vanilla.banira.client.event.BaniraClientEventHub.ModLifecycle}
     */
    public static final class ModLifecycle {
        private ModLifecycle() {
        }

        public static void onCommonSetup(@Nonnull Consumer<FMLCommonSetupEvent> callback) {
            modCommonSetupCallbacks.add(callback);
        }
    }

    // endregion

    // region Forge 事件订阅

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        fire(serverStartingCallbacks, event.getServer(), "server starting");
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        fire(serverStartedCallbacks, event.getServer(), "server started");
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        fire(serverStoppingCallbacks, event.getServer(), "server stopping");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        fire(serverTickCallbacks, event, "server tick");
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        fire(playerLoggedInCallbacks, event.getEntity(), "player logged in");
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        fire(playerLoggedOutCallbacks, event.getEntity(), "player logged out");
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        fire(playerChangedDimensionCallbacks, event, "player changed dimension");
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        LevelAccessor world = event.getLevel();
        if (!world.isClientSide()) {
            fire(worldSaveCallbacks, "world save");
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        LevelAccessor world = event.getLevel();
        if (world != null && !world.isClientSide()) {
            fire(chunkSaveCallbacks, "chunk save");
        }
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        if (player instanceof ServerPlayer s) {
            fire(playerSaveCallbacks, s, "player save");
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        fire(worldUnloadCallbacks, event, "world unload");
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        fire(worldTickCallbacks, event, "world tick");
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        fire(playerCloneCallbacks, event, "player clone");
    }

    @SubscribeEvent
    public static void onAnyPlayerEvent(PlayerEvent event) {
        fire(playerEventCallbacks, event, "player event");
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        fire(playerRightClickItemCallbacks, event, "player right click item");
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        fire(playerRightClickBlockCallbacks, event, "player right click block");
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        fire(playerEntityInteractCallbacks, event, "player entity interact");
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        fire(entityJoinLevelCallbacks, event, "entity join level");
    }

    @SubscribeEvent
    public static void onEntityTeleport(EntityTeleportEvent event) {
        fire(entityTeleportCallbacks, event, "entity teleport");
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        fire(registerCommandsCallbacks, event, "register commands");
    }

    /**
     * 由 {@link net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext#getModEventBus()} {@code addListener} 注册
     */
    public static void dispatchModCommonSetup(FMLCommonSetupEvent event) {
        fire(modCommonSetupCallbacks, event, "mod common setup");
    }

    // endregion Forge 事件订阅

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
