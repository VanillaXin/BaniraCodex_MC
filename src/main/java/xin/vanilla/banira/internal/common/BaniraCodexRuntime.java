package xin.vanilla.banira.internal.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.event.BaniraLifecycle;
import xin.vanilla.banira.api.permission.BaniraVirtualPermissionRegistry;
import xin.vanilla.banira.common.enums.EnumCommandType;
import xin.vanilla.banira.common.network.ModLoadedPresenceStore;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.config.ManagedConfigFiles;

/**
 * Banira 自身的跨加载器运行时注册，加载器事件只负责触发这些公共回调。
 */
public final class BaniraCodexRuntime {

    private static final int CONFIG_SAVE_INTERVAL_TICKS = 6000;

    private BaniraCodexRuntime() {
    }

    public static void bootstrap() {
        registerCommonLifecycle();
        registerServerLifecycle();
        registerPlayerLifecycle();
    }

    private static void registerCommonLifecycle() {
        for (EnumCommandType commandType : EnumCommandType.values()) {
            if (commandType.op()) {
                BaniraVirtualPermissionRegistry.register(commandType);
            }
        }
        BaniraLifecycle.onCommonSetup(event -> {
            CustomConfig.loadCustomConfig(false);
            ModLoadedPresenceStore.register(Banira.MOD_ID);
        });
        BaniraEventBus.Save.onWorldSave(CustomConfig::saveCustomConfig);
    }

    private static void registerServerLifecycle() {
        BaniraEventBus.Server.onStarting(event -> BaniraServerRuntime.markStarting(event.serverAs(MinecraftServer.class)));
        BaniraEventBus.Server.onStarting(event -> BaniraServerRuntime.playerDataManager().clearCache());
        BaniraEventBus.Server.onStarting(event -> AdvancementUtils.clearAdvancementData());
        BaniraEventBus.Server.onStopping(event -> BaniraServerRuntime.markStopping());

        // 自定义配置允许外部编辑，运行时定期重新读取并补齐默认值。
        BaniraEventBus.Server.onTick(event -> {
            ManagedConfigFiles.poll(ManagedConfigFiles.Scope.COMMON);
            MinecraftServer server = BaniraServerRuntime.server();
            if (server == null) return;
            if (server.getTickCount() % CONFIG_SAVE_INTERVAL_TICKS == 0) {
                if (!CustomConfig.loadCustomConfig(true)) {
                    CustomConfig.saveCustomConfig();
                }
            }
        });
    }

    private static void registerPlayerLifecycle() {
        BaniraEventBus.Save.onPlayerSave(event -> {
            if (event.uuid() != null) {
                BaniraServerRuntime.playerDataManager().saveToDisk(event.uuid());
            }
        });
        BaniraEventBus.Player.onLoggedOut(event -> {
            ServerPlayer player = event.playerAs(ServerPlayer.class);
            if (player != null) {
                PlayerUtils.removeRemoteClientDataStatus(player);
            }
        });
        BaniraEventBus.Player.onLoggedIn(event -> {
            ServerPlayer player = event.playerAs(ServerPlayer.class);
            if (player != null) {
                PacketUtils.sendPacketToPlayer(new NotificationTypesSyncToClient(ServerNotificationTypeRegistry.buildSyncEntries()), player);
            }
        });
    }

}
