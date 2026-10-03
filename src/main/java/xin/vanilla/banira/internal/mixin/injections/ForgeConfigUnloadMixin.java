package xin.vanilla.banira.internal.mixin.injections;

import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xin.vanilla.banira.internal.forge.config.ForgeConfigFile;

@Mixin(value = ConfigTracker.class, remap = false)
public abstract class ForgeConfigUnloadMixin {
    @Redirect(method = "closeConfig", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/config/ModConfig;save()V"))
    private void banira$preserveInvalidFileAndFinishUnloading(ModConfig config) {
        if (config.getConfigData() instanceof ForgeConfigFile)
            ((ForgeConfigFile) config.getConfigData()).saveOnUnload();
        else config.save();
    }
}
