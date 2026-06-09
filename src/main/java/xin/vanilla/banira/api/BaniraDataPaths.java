package xin.vanilla.banira.api;

import net.minecraft.world.level.storage.LevelResource;
import xin.vanilla.banira.internal.common.BaniraPaths;

import java.nio.file.Path;

/**
 * 子 mod 访问 Banira 数据目录的稳定入口。
 */
public final class BaniraDataPaths {

    private BaniraDataPaths() {
    }

    public static String rootDirectoryName() {
        return BaniraPaths.ROOT_DIRECTORY_NAME;
    }

    public static LevelResource worldDataDirectory() {
        return BaniraPaths.WORLD_DATA_DIRECTORY;
    }

    public static Path worldDataPath() {
        return BaniraPaths.worldDataPath();
    }

    public static Path playerDataPath() {
        return BaniraPaths.playerDataPath();
    }

    public static Path configPath() {
        return BaniraPaths.configPath();
    }
}
