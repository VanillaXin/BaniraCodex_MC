package xin.vanilla.banira.common.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.player.PlayerDataManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 子 mod 可用的服务端运行时工具；具体 server 绑定细节留在当前加载器入口内。
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
        return server != null ? server.getPlayerList().getPlayers() : Collections.emptyList();
    }

    @Nullable
    public static ServerPlayer player(@Nonnull UUID uuid) {
        MinecraftServer server = currentServer();
        return server != null ? server.getPlayerList().getPlayer(uuid) : null;
    }

    @Nonnull
    public static PlayerDataManager playerDataManager() {
        return BaniraCodex.playerDataManager;
    }
}
