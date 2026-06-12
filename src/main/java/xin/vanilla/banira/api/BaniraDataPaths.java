package xin.vanilla.banira.api;

import xin.vanilla.banira.BaniraCodex;

import java.nio.file.Path;

/**
 * 子 mod 访问 Banira 数据目录的稳定入口。
 */
public final class BaniraDataPaths {

    private BaniraDataPaths() {
    }

    public static String rootDirectoryName() {
        return BaniraCodex.VANILLA_XIN;
    }

    public static Path worldDataPath() {
        return Banira.platform().server().worldDataPath(rootDirectoryName());
    }

    public static Path playerDataPath() {
        Path worldDataPath = worldDataPath();
        return worldDataPath != null ? worldDataPath.resolve("playerdata") : null;
    }

    public static Path configPath() {
        return Banira.platform().configDir().resolve(rootDirectoryName());
    }

    public static Path vanillaPlayerDataPath() {
        return Banira.platform().server().worldPlayerDataPath();
    }
}
