package xin.vanilla.banira.internal.common;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.storage.LevelResource;
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
 * <p>服务端句柄和玩家数据管理器集中在这里，加载器入口只负责更新生命周期。</p>
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

    @Nullable
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

    public static void markStarting(@Nonnull MinecraftServer server) {
        serverInstance().key(server).value(true);
    }

    public static void markStopping() {
        serverInstance().value(false);
    }
}
