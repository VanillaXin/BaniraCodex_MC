package xin.vanilla.banira.internal.mixin.accessors;

import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xin.vanilla.banira.internal.neoforge.config.NeoForgeConfigSpecAccess;

@Mixin(value = ModConfigSpec.class, remap = false)
public interface NeoForgeConfigSpecAccessor extends NeoForgeConfigSpecAccess {
    @Override @Accessor("loadedConfig")
    void banira$bindLoadedConfig(IConfigSpec.ILoadedConfig loaded);
}
