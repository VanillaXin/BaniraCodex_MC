package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * 纹理图集重载事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraTextureReloadEvent {
    private final @Nonnull ResourceLocation atlasLocation;

    public BaniraTextureReloadEvent(@Nonnull ResourceLocation atlasLocation) {
        this.atlasLocation = atlasLocation;
    }
}
