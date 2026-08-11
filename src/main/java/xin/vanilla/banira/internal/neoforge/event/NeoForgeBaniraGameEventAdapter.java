package xin.vanilla.banira.internal.neoforge.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import xin.vanilla.banira.api.event.BaniraPlayerDimensionEvent;
import xin.vanilla.banira.api.event.BaniraPlayerEvent;
import xin.vanilla.banira.api.event.BaniraServerEvent;
import xin.vanilla.banira.api.event.BaniraWorldEvent;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

/**
 * NeoForge 游戏总线适配器，只负责把加载器事件转换成 Banira 的中立回调。
 */
public final class NeoForgeBaniraGameEventAdapter {
    private NeoForgeBaniraGameEventAdapter() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        BaniraEventBus.dispatchServerStarting(new BaniraServerEvent(event.getServer()));
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        BaniraEventBus.dispatchServerStarted(new BaniraServerEvent(event.getServer()));
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BaniraEventBus.dispatchServerStopping(new BaniraServerEvent(event.getServer()));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        BaniraEventBus.dispatchServerTick(new BaniraServerEvent(server));
        BaniraScheduler.dispatchServerTick(server);
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent.Post event) {
        BaniraEventBus.dispatchWorldTick(worldEvent(event.getLevel()));
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        BaniraEventBus.dispatchPlayerLoggedIn(playerEvent(event.getEntity()));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BaniraEventBus.dispatchPlayerLoggedOut(playerEvent(event.getEntity()));
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BaniraEventBus.dispatchPlayerChangedDimension(new BaniraPlayerDimensionEvent(
                    player,
                    player.getUUID(),
                    player.getName().getString(),
                    dimensionId(event.getFrom()),
                    dimensionId(event.getTo())
            ));
        }
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        LevelAccessor world = event.getLevel();
        if (!world.isClientSide()) {
            BaniraEventBus.dispatchWorldSave();
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkEvent.Save event) {
        LevelAccessor world = event.getLevel();
        if (world != null && !world.isClientSide()) {
            BaniraEventBus.dispatchChunkSave();
        }
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BaniraEventBus.dispatchPlayerSave(playerEvent(player));
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        BaniraEventBus.dispatchWorldUnload(worldEvent(event.getLevel()));
    }

    private static BaniraPlayerEvent playerEvent(Player player) {
        return new BaniraPlayerEvent(
                player,
                player != null ? player.getUUID() : null,
                player != null ? player.getName().getString() : null
        );
    }

    private static BaniraWorldEvent worldEvent(LevelAccessor world) {
        return new BaniraWorldEvent(world, dimensionId(world), world != null && world.isClientSide());
    }

    private static String dimensionId(LevelAccessor world) {
        return world instanceof Level ? dimensionId(((Level) world).dimension()) : "";
    }

    private static String dimensionId(ResourceKey<Level> dimension) {
        return dimension != null && dimension.location() != null ? dimension.location().toString() : "";
    }
}
