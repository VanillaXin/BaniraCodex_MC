package xin.vanilla.banira.internal.mixin.compat.jei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.fabric.compat.jei.JeiCompatibility;

@Pseudo
@Mixin(targets = {"mezz.jei.common.gui.overlay.bookmarks.BookmarkButton", "mezz.jei.gui.overlay.bookmarks.BookmarkButton"}, remap = false)
public abstract class BookmarkButtonMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(CallbackInfo callback) {
        JeiCompatibility.capture(this);
    }

    @Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$click(CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }
}
