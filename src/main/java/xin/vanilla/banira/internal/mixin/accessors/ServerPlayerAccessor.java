package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ServerPlayer.class, remap = false)
public interface ServerPlayerAccessor {
    @Accessor(value = "language")
    String banira$language();

    @Accessor(value = "language")
    void banira$language(String language);
}
