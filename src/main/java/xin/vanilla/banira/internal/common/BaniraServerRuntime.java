package xin.vanilla.banira.internal.common;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Banira 服务端运行时状态。
 *
 * <p>不同 MC 版本获取 server/tick 的方式不完全一致，统一集中在这里可减少根入口和工具类的迁移成本。</p>
 */
@Accessors(fluent = true)
public final class BaniraServerRuntime {

    private static final String LEGACY_ARTIFACT_ID = "xin.vanilla";

    @Getter
    private static final KeyValue<MinecraftServer, Boolean> serverInstance = new KeyValue<>(null, false);

    @Getter
    private static final PlayerDataManager playerDataManager = PlayerDataManager.getOrCreateInstance(
            BaniraPaths.PLAYER_DATA_PATH,
            BaniraPaths::vanillaPlayerDataPath,
            BaniraCodex.MODID,
            "",
            StringUtils.reverseBySeparatorElegant(LEGACY_ARTIFACT_ID, ".")
    );

    private BaniraServerRuntime() {
    }

    public static MinecraftServer server() {
        return serverInstance().key();
    }

    public static boolean isRunning() {
        return serverInstance().val();
    }

    @Nonnull
    public static List<ServerPlayer> players() {
        MinecraftServer server = server();
        return server != null ? server.getPlayerList().getPlayers() : Collections.emptyList();
    }

    @Nullable
    public static ServerPlayer player(@Nonnull UUID uuid) {
        MinecraftServer server = server();
        return server != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    @Nullable
    public static ResourceManager resourceManager() {
        MinecraftServer server = server();
        return server != null ? server.getResourceManager() : null;
    }

    public static void markStarting(MinecraftServer server) {
        serverInstance().key(server).value(true);
    }

    public static void markStopping() {
        serverInstance().value(false);
    }
}
