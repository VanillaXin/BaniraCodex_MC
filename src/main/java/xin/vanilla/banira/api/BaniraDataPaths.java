package xin.vanilla.banira.api;

import java.nio.file.Path;

/**
 * 子 mod 访问 Banira 数据目录的稳定入口。
 */
public final class BaniraDataPaths {

    private BaniraDataPaths() {
    }

    public static String rootDirectoryName() {
        return Banira.platform().pathService().rootDirectoryName();
    }

    public static Path worldDataPath() {
        return Banira.platform().pathService().worldDataPath();
    }

    public static Path playerDataPath() {
        return Banira.platform().pathService().playerDataPath();
    }

    public static Path configPath() {
        return Banira.platform().pathService().configPath();
    }

    public static Path vanillaPlayerDataPath() {
        return Banira.platform().pathService().vanillaPlayerDataPath();
    }
}
