package xin.vanilla.banira.common.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.player.PlayerDataManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 子 mod 可用的服务端运行时入口；具体状态存储方式由当前版本内部实现决定。
 */
public final class BaniraServerUtils {
    private BaniraServerUtils() {
    }

    public static boolean isRunning() {
        return BaniraCodex.serverInstance().val();
    }

    @Nullable
    public static MinecraftServer currentServer() {
        return BaniraCodex.serverInstance().key();
    }

    @Nonnull
    public static List<ServerPlayer> players() {
        MinecraftServer server = currentServer();
        return server == null ? Collections.emptyList() : server.getPlayerList().getPlayers();
    }

    @Nullable
    public static ServerPlayer player(@Nonnull UUID uuid) {
        MinecraftServer server = currentServer();
        return server == null ? null : server.getPlayerList().getPlayer(uuid);
    }

    @Nonnull
    public static PlayerDataManager playerDataManager() {
        return BaniraCodex.playerDataManager;
    }
}
