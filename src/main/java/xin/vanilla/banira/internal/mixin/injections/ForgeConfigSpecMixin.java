package xin.vanilla.banira.internal.mixin.injections;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.forge.config.ForgeConfigFile;

@Mixin(value = ForgeConfigSpec.class, remap = false)
public abstract class ForgeConfigSpecMixin {
    @Inject(method = "isCorrect", at = @At("HEAD"), cancellable = true)
    private void banira$alreadyValidated(CommentedConfig config, CallbackInfoReturnable<Boolean> ci) {
        if (config instanceof ForgeConfigFile && ((ForgeConfigFile) config).belongsTo((ForgeConfigSpec) (Object) this)) {
            // The file validates registered values before publication, preserving unrelated entries and comments.
            ci.setReturnValue(true);
        }
    }
}
