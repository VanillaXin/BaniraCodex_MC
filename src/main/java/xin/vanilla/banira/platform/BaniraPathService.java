package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * 统一数据路径服务；不同 MC 版本的世界目录 API 差异留给 platform/internal 实现。
 */
public interface BaniraPathService {

    @Nonnull
    String rootDirectoryName();

    /**
     * 加载器的 config 根目录，子 mod 可在其下建立自己的目录。
     */
    @Nonnull
    Path gameConfigPath();

    @Nonnull
    Path configPath();

    @Nonnull
    Path worldDataPath();

    @Nonnull
    Path playerDataPath();

    @Nonnull
    Path vanillaPlayerDataPath();
}
