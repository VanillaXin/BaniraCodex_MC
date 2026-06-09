package xin.vanilla.banira.internal.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import xin.vanilla.banira.platform.BaniraPlatforms;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Banira 自身数据路径集中点，避免各加载器分支在根入口里重复拼路径。
 */
public final class BaniraPaths {

    public static final String ROOT_DIRECTORY_NAME = "vanilla.xin";
    public static final LevelResource WORLD_DATA_DIRECTORY = new LevelResource(ROOT_DIRECTORY_NAME);

    public static final Supplier<Path> WORLD_DATA_PATH = BaniraPaths::worldDataPath;
    public static final Supplier<Path> PLAYER_DATA_PATH = BaniraPaths::playerDataPath;
    public static final Supplier<Path> CONFIG_PATH = BaniraPaths::configPath;

    private BaniraPaths() {
    }

    public static Path worldDataPath() {
        return requireServer().getWorldPath(WORLD_DATA_DIRECTORY);
    }

    public static Path playerDataPath() {
        return worldDataPath().resolve("playerdata");
    }

    public static Path vanillaPlayerDataPath() {
        return requireServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
    }

    public static Path configPath() {
        return BaniraPlatforms.get().configDir().resolve(ROOT_DIRECTORY_NAME);
    }

    private static MinecraftServer requireServer() {
        MinecraftServer server = BaniraServerRuntime.server();
        if (server == null) {
            throw new IllegalStateException("Banira server is not available yet");
        }
        return server;
    }
}
