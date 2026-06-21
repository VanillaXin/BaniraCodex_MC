package xin.vanilla.banira.api.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 世界事件的加载器中立表示；原生 world 仅作为内部句柄保留。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraWorldEvent {
    @Nullable
    private final Object world;
    @Nonnull
    private final String dimensionId;
    private final boolean clientSide;

    public BaniraWorldEvent(@Nullable Object world, @Nullable String dimensionId, boolean clientSide) {
        this.world = world;
        this.dimensionId = dimensionId != null ? dimensionId : "";
        this.clientSide = clientSide;
    }

    @Nullable
    public <T> T worldAs(@Nonnull Class<T> type) {
        return type.isInstance(world) ? type.cast(world) : null;
    }
}
