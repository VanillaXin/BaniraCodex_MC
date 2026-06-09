package xin.vanilla.banira.internal.common;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.StringUtils;

/**
 * Banira 服务端运行时状态。
 *
 * <p>不同 MC 版本获取 server/tick 的方式不完全一致，统一集中在这里可减少根入口和工具类的迁移成本。</p>
 */
@Accessors(fluent = true)
public final class BaniraServerRuntime {

    @Getter
    private static final KeyValue<MinecraftServer, Boolean> serverInstance = new KeyValue<>(null, false);

    @Getter
    private static final PlayerDataManager playerDataManager = PlayerDataManager.getOrCreateInstance(
            BaniraCodex.BANIRA_PLAYER_DATA_PATH,
            () -> serverInstance().key().getWorldPath(LevelResource.PLAYER_DATA_DIR),
            BaniraCodex.MODID,
            "",
            StringUtils.reverseBySeparatorElegant(BaniraCodex.ARTIFACT_ID, ".")
    );

    private BaniraServerRuntime() {
    }

    public static MinecraftServer server() {
        return serverInstance().key();
    }

    public static boolean isRunning() {
        return serverInstance().val();
    }

    public static void markStarting(MinecraftServer server) {
        serverInstance().key(server).value(true);
    }

    public static void markStopping() {
        serverInstance().value(false);
    }
}
