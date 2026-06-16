package xin.vanilla.banira.internal.forge.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

/**
 * Forge 游戏总线适配器，只负责把 Forge 事件转换成 Banira 的中立回调。
 */
public final class ForgeBaniraGameEventAdapter {
    private ForgeBaniraGameEventAdapter() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        BaniraEventBus.dispatchServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        BaniraEventBus.dispatchServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BaniraEventBus.dispatchServerStopping(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = BaniraServerRuntime.server();
        if (event.phase != TickEvent.Phase.END || server == null) {
            return;
        }
        BaniraEventBus.dispatchServerTick(server);
        BaniraScheduler.dispatchServerTick(server);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BaniraEventBus.dispatchWorldTick(event.level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        BaniraEventBus.dispatchPlayerLoggedIn(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BaniraEventBus.dispatchPlayerLoggedOut(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BaniraEventBus.dispatchPlayerChangedDimension(player, event.getFrom(), event.getTo());
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
            BaniraEventBus.dispatchPlayerSave(player);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        BaniraEventBus.dispatchWorldUnload(event.getLevel());
    }
}
