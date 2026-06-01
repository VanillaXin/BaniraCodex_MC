package xin.vanilla.banira.client.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.util.ResourceLocation;

@Getter
@Accessors(fluent = true)
public final class BaniraTextureReloadEvent {
    private final ResourceLocation atlasLocation;

    public BaniraTextureReloadEvent(ResourceLocation atlasLocation) {
        this.atlasLocation = atlasLocation;
    }
}
