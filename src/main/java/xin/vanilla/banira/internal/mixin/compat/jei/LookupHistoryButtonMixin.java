package xin.vanilla.banira.internal.mixin.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.gui.input.UserInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.fabric.compat.jei.JeiCompatibility;

/** 捕获 JEI 查询历史按钮，并在被接管时屏蔽原生点击。 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButton", remap = false)
public abstract class LookupHistoryButtonMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(IDrawable offIcon, IDrawable onIcon,
                                IClientConfig clientConfig, CallbackInfo callback) {
        JeiCompatibility.captureLookupHistory(this, offIcon, onIcon, clientConfig);
    }

    @Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$blockNativeClick(UserInput input,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }
}
