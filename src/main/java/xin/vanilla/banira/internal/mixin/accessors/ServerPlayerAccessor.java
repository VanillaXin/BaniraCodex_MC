package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerAccessor {
    @Accessor("language")
    String language();

    @Accessor("language")
    void language(String language);
}
