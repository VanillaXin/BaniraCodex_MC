package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.command.BaniraCommandAccess;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.server.BaniraServerAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.nio.file.Path;
import java.util.function.Supplier;

@Accessors(fluent = true)
public final class BaniraCodex {
    public static final String MODID = Banira.MOD_ID;

    @Deprecated
    public static final String ARTIFACT_ID = "xin.vanilla";

    public static final String VANILLA_XIN = "vanilla.xin";

    public static final Supplier<Path> BANIRA_WORLD_DATA_PATH = BaniraCodex::baniraWorldDataPath;
    public static final Supplier<Path> BANIRA_PLAYER_DATA_PATH = BaniraCodex::baniraPlayerDataPath;
    public static final Supplier<Path> BANIRA_CONFIG_PATH = () -> BaniraPlatforms.get().configDir().resolve(VANILLA_XIN);

    @Getter
    private final static KeyValue<MinecraftServer, Boolean> serverInstance = new KeyValue<>(null, false);

    public static final PlayerDataManager playerDataManager = PlayerDataManager.getOrCreateInstance(
            BANIRA_PLAYER_DATA_PATH,
            () -> BaniraServerAccess.worldPlayerDataPath(),
            MODID,
            "",
            StringUtils.reverseBySeparatorElegant(ARTIFACT_ID, ".")
    );

    private static volatile boolean commonBootstrapped = false;

    private BaniraCodex() {
    }

    private static Path baniraWorldDataPath() {
        return BaniraServerAccess.worldDataPath(VANILLA_XIN);
    }

    private static Path baniraPlayerDataPath() {
        Path worldDataPath = baniraWorldDataPath();
        return worldDataPath != null ? worldDataPath.resolve("playerdata") : null;
    }

    /**
     * Registers loader-neutral runtime callbacks. Loader entry points must install a platform before calling this.
     */
    public static void bootstrapCommon() {
        if (commonBootstrapped) return;
        commonBootstrapped = true;
        registerBaniraEvent();
    }

    private static void registerBaniraEvent() {
        BaniraEventBus.ModLifecycle.onCommonSetup(() -> {
            CustomConfig.loadCustomConfig(false);
            ModLoadedPresence.register(MODID);
        });
        BaniraCommandAccess.onRegisterDispatcher(BaniraCommand::register);

        BaniraEventBus.Server.onStarting(server -> serverInstance().key(server).value(true));
        BaniraEventBus.Server.onStarting(server -> playerDataManager.clearCache());
        BaniraEventBus.Server.onStarting(server -> AdvancementUtils.clearAdvancementData());
        BaniraEventBus.Server.onStopping(server -> serverInstance().value(false));

        final int CONFIG_SAVE_INTERVAL_TICKS = 6000;
        BaniraEventBus.Server.onTickEnd(() -> {
            long tick = BaniraServerAccess.tickCount();
            if (tick > 0 && tick % CONFIG_SAVE_INTERVAL_TICKS == 0) {
                if (!CustomConfig.loadCustomConfig(true)) {
                    CustomConfig.saveCustomConfig();
                }
            }
        });
        BaniraEventBus.Save.onWorldSave(CustomConfig::saveCustomConfig);

        BaniraEventBus.Save.onPlayerSave(player ->
                playerDataManager.saveToDisk(PlayerUtils.getPlayerUUID(player))
        );
        BaniraEventBus.Player.onLoggedOut(player -> {
            if (player instanceof ServerPlayerEntity) {
                PlayerUtils.removeRemoteClientDataStatus(player);
            }
        });
        BaniraEventBus.Player.onLoggedIn(player -> {
            if (player instanceof ServerPlayerEntity) {
                ServerPlayerEntity sp = (ServerPlayerEntity) player;
                PacketUtils.sendPacketToPlayer(new NotificationTypesSyncToClient(ServerNotificationTypeRegistry.buildSyncEntries()), sp);
            }
        });
    }
}
