package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

/**
 * 纹理图集重载事件；公开 API 用字符串 id 避免绑定不同版本的纹理位置类。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraTextureReloadEvent {
    private final @Nonnull String atlasLocation;

    public BaniraTextureReloadEvent(@Nonnull String atlasLocation) {
        this.atlasLocation = atlasLocation;
    }
}
