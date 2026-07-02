package xin.vanilla.banira.api.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 玩家相关公共事件对象；原生玩家对象只作为不稳定句柄保留在版本适配边界内使用。
 */
@Getter
@Accessors(fluent = true)
public class BaniraPlayerEvent {
    @Nullable
    private final Object player;
    @Nullable
    private final UUID uuid;
    @Nonnull
    private final String uuidString;
    @Nonnull
    private final String name;

    public BaniraPlayerEvent(@Nullable Object player, @Nullable UUID uuid, @Nullable String name) {
        this.player = player;
        this.uuid = uuid;
        this.uuidString = uuid != null ? uuid.toString() : "";
        this.name = name != null ? name : "";
    }

    public boolean hasPlayer() {
        return player != null;
    }

    @Nullable
    public <T> T playerAs(@Nonnull Class<T> type) {
        return type.isInstance(player) ? type.cast(player) : null;
    }
}
