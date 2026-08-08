package xin.vanilla.banira;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.permission.BaniraVirtualPermissionRegistry;
import xin.vanilla.banira.command.BaniraCommand;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.command.BaniraCommandAccess;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;
import xin.vanilla.banira.internal.server.BaniraServerAccess;
import xin.vanilla.banira.internal.server.ServerSenderAccess;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.nio.file.Path;
import java.util.Arrays;
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
        Arrays.stream(EnumCommandType.values())
                .filter(EnumCommandType::op)
                .forEach(BaniraVirtualPermissionRegistry::register);
        registerBaniraEvent();
    }

    private static void registerBaniraEvent() {
        BaniraEventBus.ModLifecycle.onCommonSetup(event -> event.enqueueWork(() -> {
            CustomConfig.loadCustomConfig(false);
            ModLoadedPresence.register(MODID);
        }));
        BaniraCommandAccess.onRegisterDispatcher(BaniraCommand::register);

        BaniraEventBus.Server.onStarting(event -> serverInstance().key(event.serverAs(MinecraftServer.class)).value(true));
        BaniraEventBus.Server.onStarting(event -> playerDataManager.clearCache());
        BaniraEventBus.Server.onStarting(event -> AdvancementUtils.clearAdvancementData());
        BaniraEventBus.Server.onStopping(event -> serverInstance().value(false));

        final int CONFIG_SAVE_INTERVAL_TICKS = 6000;
        BaniraEventBus.Server.onTick(event -> {
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
            long tick = BaniraServerAccess.tickCount();
            if (tick > 0 && tick % CONFIG_SAVE_INTERVAL_TICKS == 0) {
                if (!CustomConfig.loadCustomConfig(true)) {
                    CustomConfig.saveCustomConfig();
                }
            }
        });
        BaniraEventBus.Save.onWorldSave(CustomConfig::saveCustomConfig);

        BaniraEventBus.Save.onPlayerSave(event -> {
            if (event.uuid() != null) {
                playerDataManager.saveToDisk(event.uuid());
            }
        });
        BaniraEventBus.Player.onLoggedOut(event -> ServerSenderAccess.removeRemoteClientDataStatus(event.player()));
        BaniraEventBus.Player.onLoggedIn(event -> ServerSenderAccess.sendPacket(
                event.player(),
                new NotificationTypesSyncToClient(ServerNotificationTypeRegistry.buildSyncEntries())
        ));
    }
}
