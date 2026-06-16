package xin.vanilla.banira;

import lombok.experimental.Accessors;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraLifecycle;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.config.BaniraConfig;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.common.BaniraCodexRuntime;
import xin.vanilla.banira.internal.common.BaniraPaths;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.fabric.platform.FabricBaniraPlatform;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.nio.file.Path;
import java.util.function.Supplier;

@Accessors(fluent = true)
public class BaniraCodex implements ModInitializer {

    public static final String MODID = Banira.MOD_ID;
    @Deprecated
    public static final String ARTIFACT_ID = "xin.vanilla";
    public static final String VANILLA_XIN = BaniraPaths.ROOT_DIRECTORY_NAME;

    /**
     * 旧工具类仍会读取这些路径；新代码优先使用 Banira.platform().pathService()。
     */
    public static final Supplier<Path> BANIRA_WORLD_DATA_PATH = BaniraPaths.WORLD_DATA_PATH;
    public static final Supplier<Path> BANIRA_PLAYER_DATA_PATH = BaniraPaths.PLAYER_DATA_PATH;
    public static final Supplier<Path> BANIRA_CONFIG_PATH = BaniraPaths.CONFIG_PATH;

    public static final PlayerDataManager playerDataManager = BaniraServerRuntime.playerDataManager();

    @Override
    public void onInitialize() {
        BaniraPlatforms.install(new FabricBaniraPlatform());
        BaniraConfig.register(CommonConfig.class, Banira.MOD_ID);
        BaniraConfig.register(ClientConfig.class, Banira.MOD_ID);
        NetworkInit.register();
        registerFabricEvents();
        BaniraCodexRuntime.bootstrap();
        BaniraLifecycle.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());
    }

    public static KeyValue<net.minecraft.server.MinecraftServer, Boolean> serverInstance() {
        return BaniraServerRuntime.serverInstance();
    }

    private void registerFabricEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(BaniraEventBus::dispatchServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(BaniraEventBus::dispatchServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(BaniraEventBus::dispatchServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BaniraEventBus.dispatchServerTick(server);
            BaniraScheduler.dispatchServerTick(server);
        });
        ServerTickEvents.END_WORLD_TICK.register(BaniraEventBus::dispatchWorldTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                BaniraEventBus.dispatchPlayerLoggedIn(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                BaniraEventBus.dispatchPlayerLoggedOut(handler.player));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> BaniraEventBus.dispatchWorldUnload(world));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BaniraEventBus.dispatchWorldSave();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BaniraEventBus.dispatchPlayerSave(player);
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> BaniraCommand.register(dispatcher));
    }
}
