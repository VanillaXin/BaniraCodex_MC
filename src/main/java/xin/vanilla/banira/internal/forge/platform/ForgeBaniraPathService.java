package xin.vanilla.banira.internal.forge.platform;

import xin.vanilla.banira.internal.server.BaniraServerAccess;
import xin.vanilla.banira.platform.BaniraPathService;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Forge 1.16.5 路径实现；世界目录 API 的版本差异留在 internal 层。
 */
public final class ForgeBaniraPathService implements BaniraPathService {
    private static final String ROOT_DIRECTORY_NAME = "vanilla.xin";

    private final Supplier<Path> configDir;

    public ForgeBaniraPathService(@Nonnull Supplier<Path> configDir) {
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
