package xin.vanilla.banira.internal.forge.event;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import xin.vanilla.banira.api.event.BaniraPlayerDimensionEvent;
import xin.vanilla.banira.api.event.BaniraPlayerEvent;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.internal.command.BaniraCommandAccess;

public final class ForgeBaniraEventBridge {
    private ForgeBaniraEventBridge() {
    }

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        BaniraEventBus.dispatchServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(FMLServerStartedEvent event) {
        BaniraEventBus.dispatchServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        BaniraEventBus.dispatchServerStopping(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BaniraEventBus.dispatchServerTickEnd();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BaniraEventBus.dispatchClientTickEnd();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        BaniraEventBus.dispatchPlayerLoggedIn(playerEvent(event.getPlayer()));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        BaniraEventBus.dispatchPlayerLoggedOut(playerEvent(event.getPlayer()));
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
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
    public static void onWorldSave(WorldEvent.Save event) {
        IWorld world = event.getWorld();
        if (world != null && !world.isClientSide()) {
            BaniraEventBus.dispatchWorldSave();
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkEvent.Save event) {
        IWorld world = event.getWorld();
        if (world != null && !world.isClientSide()) {
            BaniraEventBus.dispatchChunkSave();
        }
    }

    @SubscribeEvent
    public static void onPlayerSaveToFile(PlayerEvent.SaveToFile event) {
        PlayerEntity player = event.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            BaniraEventBus.dispatchPlayerSave(playerEvent(player));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        BaniraCommandAccess.dispatchRegisterDispatcher(event.getDispatcher());
    }

    private static BaniraPlayerEvent playerEvent(PlayerEntity player) {
        return new BaniraPlayerEvent(
                player,
                player != null ? player.getUUID() : null,
                player != null ? player.getName().getString() : null
        );
    }

    private static String dimensionId(RegistryKey<World> dimension) {
        return dimension != null && dimension.location() != null ? dimension.location().toString() : "";
    }
}
