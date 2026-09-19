package xin.vanilla.banira.internal.mixin.injections;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.forge.config.ForgeConfigAdapter;
import xin.vanilla.banira.internal.forge.config.ForgeConfigFile;

@Mixin(value = ForgeConfigSpec.class, remap = false)
public abstract class ForgeConfigSpecMixin {
    @Shadow private com.electronwill.nightconfig.core.Config childConfig;

    @Inject(method = "setConfig", at = @At("HEAD"))
    private void banira$releaseReplacedFile(CommentedConfig next, CallbackInfo ci) {
        ForgeConfigAdapter.releaseReplacedFile((ForgeConfigSpec) (Object) this, childConfig, next);
    }

    @Inject(method = "isCorrect", at = @At("HEAD"), cancellable = true)
    private void banira$alreadyValidated(CommentedConfig config, CallbackInfoReturnable<Boolean> ci) {
        if (config instanceof ForgeConfigFile && ((ForgeConfigFile) config).belongsTo((ForgeConfigSpec) (Object) this)) {
            // The file validates registered values before publication, preserving unrelated entries and comments.
            ci.setReturnValue(true);
        }
    }
}
