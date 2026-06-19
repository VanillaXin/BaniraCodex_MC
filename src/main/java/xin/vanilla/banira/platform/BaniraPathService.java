package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * 统一数据路径服务；不同 MC 版本的世界目录 API 差异留给 platform/internal 实现。
 */
public interface BaniraPathService {

    @Nonnull
    String rootDirectoryName();

    @Nonnull
    Path configPath();

    @Nonnull
    Path worldDataPath();

    @Nonnull
    Path playerDataPath();

    @Nonnull
    Path vanillaPlayerDataPath();
}
