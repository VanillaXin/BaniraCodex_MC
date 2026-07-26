package xin.vanilla.banira.internal.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.event.*;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.command.BaniraCommandAccess;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.fabric.platform.FabricBaniraPlatform;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * Fabric 1.16 公共入口，只负责安装平台并桥接加载器生命周期。
 */
public final class FabricBaniraCodexEntry implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!BaniraPlatforms.isInstalled()) {
            BaniraPlatforms.install(new FabricBaniraPlatform());
        }
        BaniraPlatforms.get().configService().register(CommonConfig.class, BaniraCodex.MODID);
        BaniraPlatforms.get().configService().register(ClientConfig.class, BaniraCodex.MODID);

        registerFabricEvents();
        BaniraScheduler.init();
        NetworkInit.register();
        BaniraCodex.bootstrapCommon();
        BaniraEventBus.dispatchCommonSetup(BaniraCommonSetupEvent.immediate());
    }

    private static void registerFabricEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                BaniraEventBus.dispatchServerStarting(new BaniraServerEvent(server)));
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                BaniraEventBus.dispatchServerStarted(new BaniraServerEvent(server)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BaniraEventBus.dispatchWorldSave();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BaniraEventBus.dispatchPlayerSave(playerEvent(player));
            }
            BaniraEventBus.dispatchServerStopping(new BaniraServerEvent(server));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BaniraEventBus.dispatchServerTick(new BaniraServerEvent(server));
            BaniraScheduler.dispatchServerTick(server);
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> BaniraEventBus.dispatchWorldTick(worldEvent(world)));
        ServerWorldEvents.UNLOAD.register((server, world) -> BaniraEventBus.dispatchWorldUnload(worldEvent(world)));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                BaniraEventBus.dispatchPlayerLoggedIn(playerEvent(handler.player)));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            BaniraPlayerEvent event = playerEvent(handler.player);
            BaniraEventBus.dispatchPlayerSave(event);
            BaniraEventBus.dispatchPlayerLoggedOut(event);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                BaniraEventBus.dispatchPlayerChangedDimension(new BaniraPlayerDimensionEvent(
                        player,
                        player.getUUID(),
                        player.getName().getString(),
                        dimensionId(origin.dimension()),
                        dimensionId(destination.dimension())
                )));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                BaniraCommandAccess.dispatchRegisterDispatcher(dispatcher));
    }

    private static BaniraPlayerEvent playerEvent(ServerPlayer player) {
        return new BaniraPlayerEvent(player, player != null ? player.getUUID() : null,
                player != null ? player.getName().getString() : null);
    }

    private static BaniraWorldEvent worldEvent(ServerLevel world) {
        return new BaniraWorldEvent(world, dimensionId(world != null ? world.dimension() : null), false);
    }

    private static String dimensionId(ResourceKey<Level> dimension) {
        return dimension != null && dimension.location() != null ? dimension.location().toString() : "";
    }
}
