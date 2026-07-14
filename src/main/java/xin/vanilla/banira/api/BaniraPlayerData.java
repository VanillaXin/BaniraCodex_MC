package xin.vanilla.banira.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * 子 mod 读写玩家持久数据的稳定入口。
 */
public final class BaniraPlayerData {
    private BaniraPlayerData() {
    }

    @Nonnull
    public static <T> T getOrCreate(@Nonnull UUID playerUuid, @Nonnull String modId, @Nonnull Class<T> dataType) {
        Objects.requireNonNull(dataType, "dataType");
        Object data = Banira.platform().playerDataService().getOrCreate(playerUuid, modId);
        if (!dataType.isInstance(data)) {
            throw new IllegalStateException("Player data is not " + dataType.getName() + ": " + data.getClass().getName());
        }
        return dataType.cast(data);
    }

    public static void put(@Nonnull UUID playerUuid, @Nonnull String modId, @Nullable Object data) {
        Banira.platform().playerDataService().put(playerUuid, modId, data);
    }
}
