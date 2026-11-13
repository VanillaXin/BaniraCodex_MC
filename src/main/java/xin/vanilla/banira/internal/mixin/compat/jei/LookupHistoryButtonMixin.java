package xin.vanilla.banira.internal.mixin.compat.jei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.fabric.compat.jei.JeiCompatibility;

@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.history.LookupHistoryButton", remap = false)
public abstract class LookupHistoryButtonMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(CallbackInfo callback) {
        JeiCompatibility.captureHistory(this);
    }

    @Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$click(CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }
}
