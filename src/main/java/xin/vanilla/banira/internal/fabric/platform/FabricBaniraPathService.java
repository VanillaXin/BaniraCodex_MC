package xin.vanilla.banira.internal.fabric.platform;

import xin.vanilla.banira.internal.server.BaniraServerAccess;
import xin.vanilla.banira.platform.BaniraPathService;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Fabric 1.16 路径实现；世界存档目录仍由版本适配服务提供。
 */
public final class FabricBaniraPathService implements BaniraPathService {
    private static final String ROOT_DIRECTORY_NAME = "vanilla.xin";
    private final Supplier<Path> configDir;

    public FabricBaniraPathService(@Nonnull Supplier<Path> configDir) {
        this.configDir = configDir;
    }

    @Override
    public String rootDirectoryName() {
        return ROOT_DIRECTORY_NAME;
    }

    @Override
    public Path configPath() {
        return configDir.get().resolve(rootDirectoryName());
    }

    @Override
    public Path worldDataPath() {
        Path path = BaniraServerAccess.worldDataPath(rootDirectoryName());
        return path != null ? path : Paths.get("world", rootDirectoryName());
    }

    @Override
    public Path playerDataPath() {
        return worldDataPath().resolve("playerdata");
    }

    @Override
    public Path vanillaPlayerDataPath() {
        Path path = BaniraServerAccess.worldPlayerDataPath();
        return path != null ? path : Paths.get("world", "playerdata");
    }
}
