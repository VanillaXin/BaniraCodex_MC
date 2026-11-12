package xin.vanilla.banira.internal.mixin.compat.jei;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.forge.compat.jei.JeiCompatibility;

/**
 * 捕获 JEI 书签按钮并为代理入口暴露原点击动作。
 */
@Pseudo
@Mixin(targets = {"mezz.jei.common.gui.overlay.bookmarks.BookmarkButton", "mezz.jei.gui.overlay.bookmarks.BookmarkButton"}, remap = false)
public abstract class BookmarkButtonMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(CallbackInfo callback) {
        JeiCompatibility.capture(this);
    }

    @Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$blockNativeClick(CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }

}
