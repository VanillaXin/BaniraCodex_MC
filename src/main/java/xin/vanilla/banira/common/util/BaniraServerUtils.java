package xin.vanilla.banira.common.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 子 mod 可用的服务端运行时工具；不同版本的 server 绑定细节保留在 internal runtime。
 */
public final class BaniraServerUtils {
    private BaniraServerUtils() {
    }

    public static boolean isRunning() {
        return BaniraServerRuntime.isRunning();
    }

    @Nullable
    public static MinecraftServer currentServer() {
        return BaniraServerRuntime.server();
    }

    @Nonnull
    public static List<ServerPlayer> players() {
        return BaniraServerRuntime.players();
    }

    @Nullable
    public static ServerPlayer player(@Nonnull UUID uuid) {
        return BaniraServerRuntime.player(uuid);
    }

    @Nonnull
    public static PlayerDataManager playerDataManager() {
        return BaniraServerRuntime.playerDataManager();
    }
}
