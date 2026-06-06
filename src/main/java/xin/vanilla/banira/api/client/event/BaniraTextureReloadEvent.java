package xin.vanilla.banira.api.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * 纹理图集重载事件。
 */
@Getter
@Accessors(fluent = true)
public final class BaniraTextureReloadEvent {
    private final @Nonnull ResourceLocation atlasLocation;
    private final @Nonnull Object nativeEvent;

    public BaniraTextureReloadEvent(@Nonnull ResourceLocation atlasLocation, @Nonnull Object nativeEvent) {
        this.atlasLocation = atlasLocation;
        this.nativeEvent = nativeEvent;
    }
}
