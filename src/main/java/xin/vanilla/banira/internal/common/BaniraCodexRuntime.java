package xin.vanilla.banira.internal.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.event.BaniraLifecycle;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.network.packet.NotificationTypesSyncToClient;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap;
import xin.vanilla.banira.internal.config.CustomConfig;

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
        registerClientLifecycle();
    }

    private static void registerCommonLifecycle() {
        BaniraLifecycle.onCommonSetup(event -> {
            CustomConfig.loadCustomConfig(false);
            ModLoadedPresence.register(BaniraCodex.MODID);
        });
        BaniraEventBus.Save.onWorldSave(CustomConfig::saveCustomConfig);
    }

    private static void registerServerLifecycle() {
        BaniraEventBus.Server.onStarting(server -> BaniraCodex.serverInstance().key(server).value(true));
        BaniraEventBus.Server.onStarting(server -> BaniraCodex.playerDataManager.clearCache());
        BaniraEventBus.Server.onStarting(server -> AdvancementUtils.clearAdvancementData());
        BaniraEventBus.Server.onStopping(server -> BaniraCodex.serverInstance().value(false));

        // 自定义配置允许外部编辑，运行时定期重新读取并补齐默认值。
        BaniraEventBus.Server.onTick(event -> {
            MinecraftServer server = BaniraCodex.serverInstance().key();
            if (server == null) return;
            if (server.getTickCount() % CONFIG_SAVE_INTERVAL_TICKS == 0) {
                if (!CustomConfig.loadCustomConfig(true)) {
                    CustomConfig.saveCustomConfig();
                }
            }
        });
    }

    private static void registerPlayerLifecycle() {
        BaniraEventBus.Save.onPlayerSave(player ->
                BaniraCodex.playerDataManager.saveToDisk(PlayerUtils.getPlayerUUID(player))
        );
        BaniraEventBus.Player.onLoggedOut(player -> {
            if (player instanceof ServerPlayer) {
                PlayerUtils.removeRemoteClientDataStatus(player);
            }
        });
        BaniraEventBus.Player.onLoggedIn(player -> {
            if (player instanceof ServerPlayer sp) {
                PacketUtils.sendPacketToPlayer(new NotificationTypesSyncToClient(ServerNotificationTypeRegistry.buildSyncEntries()), sp);
            }
        });
    }

    private static void registerClientLifecycle() {
        if (EnvironmentUtils.isClient()) {
            BaniraCodexClientBootstrap.init();
        }
    }
}
