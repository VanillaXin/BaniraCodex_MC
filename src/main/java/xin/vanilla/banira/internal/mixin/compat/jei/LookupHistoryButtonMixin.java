package xin.vanilla.banira.internal.mixin.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.common.config.IClientConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.forge.compat.jei.JeiCompatibility;

/** 捕获 JEI 查询历史控制器，并在被接管时屏蔽原生点击。 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButtonController",
        remap = false)
public abstract class LookupHistoryButtonMixin {
    @Shadow @Final private IDrawable offIcon;
    @Shadow @Final private IDrawable onIcon;
    @Shadow @Final private IClientConfig clientConfig;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(IClientConfig clientConfig, CallbackInfo callback) {
        JeiCompatibility.captureLookupHistory(this, offIcon, onIcon, this.clientConfig);
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$blockNativeClick(IJeiUserInput input,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }
}
