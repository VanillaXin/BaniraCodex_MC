package xin.vanilla.banira;

import lombok.experimental.Accessors;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.api.event.*;
import xin.vanilla.banira.command.BaniraCommand;
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
        BaniraConfigs.register(CommonConfig.class, Banira.MOD_ID);
        BaniraConfigs.register(ClientConfig.class, Banira.MOD_ID);
        NetworkInit.register();
        registerFabricEvents();
        BaniraCodexRuntime.bootstrap();
        BaniraLifecycle.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());
    }

    public static KeyValue<net.minecraft.server.MinecraftServer, Boolean> serverInstance() {
        return BaniraServerRuntime.serverInstance();
    }

    private void registerFabricEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                BaniraEventBus.dispatchServerStarting(new BaniraServerEvent(server)));
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                BaniraEventBus.dispatchServerStarted(new BaniraServerEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                BaniraEventBus.dispatchServerStopping(new BaniraServerEvent(server)));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BaniraEventBus.dispatchServerTick(new BaniraServerEvent(server));
            BaniraScheduler.dispatchServerTick(server);
        });
        ServerTickEvents.END_WORLD_TICK.register(world ->
                BaniraEventBus.dispatchWorldTick(worldEvent(world)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                BaniraEventBus.dispatchPlayerLoggedIn(playerEvent(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BaniraPlayerEvent event = playerEvent(handler.player);
            BaniraEventBus.dispatchPlayerSave(event);
            BaniraEventBus.dispatchPlayerLoggedOut(event);
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) ->
                BaniraEventBus.dispatchWorldUnload(worldEvent(world)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BaniraEventBus.dispatchWorldSave();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BaniraEventBus.dispatchPlayerSave(playerEvent(player));
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> BaniraCommand.register(dispatcher));
    }

    private static BaniraPlayerEvent playerEvent(ServerPlayer player) {
        return new BaniraPlayerEvent(
                player,
                player != null ? player.getUUID() : null,
                player != null ? player.getName().getString() : null
        );
    }

    private static BaniraWorldEvent worldEvent(Level world) {
        return new BaniraWorldEvent(world, dimensionId(world), world != null && world.isClientSide());
    }

    private static String dimensionId(Level world) {
        return world != null ? dimensionId(world.dimension()) : "";
    }

    private static String dimensionId(ResourceKey<Level> dimension) {
        return dimension != null && dimension.location() != null ? dimension.location().toString() : "";
    }
}
