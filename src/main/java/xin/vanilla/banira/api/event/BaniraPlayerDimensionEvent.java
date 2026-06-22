package xin.vanilla.banira.api.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 玩家跨维度事件。维度用字符串 id 表示，避免公共事件签名绑定不同版本的 RegistryKey 类型。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraPlayerDimensionEvent extends BaniraPlayerEvent {
    @Nonnull
    private final String fromDimensionId;
    @Nonnull
    private final String toDimensionId;

    public BaniraPlayerDimensionEvent(@Nullable Object player, @Nullable UUID uuid, @Nullable String name,
                                      @Nullable String fromDimensionId, @Nullable String toDimensionId) {
        super(player, uuid, name);
        this.fromDimensionId = fromDimensionId != null ? fromDimensionId : "";
        this.toDimensionId = toDimensionId != null ? toDimensionId : "";
    }
}
