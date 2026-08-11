package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 跨版本玩家持久数据服务；底层数据类型由对应 MC 分支校验。
 */
public interface BaniraPlayerDataService {
    @Nonnull
    Object getOrCreate(@Nonnull UUID playerUuid, @Nonnull String modId);

    void put(@Nonnull UUID playerUuid, @Nonnull String modId, @Nullable Object data);

    /**
     * 立即持久化指定玩家所有已修改的 Banira 数据。
     */
    void flush(@Nonnull UUID playerUuid);
}
