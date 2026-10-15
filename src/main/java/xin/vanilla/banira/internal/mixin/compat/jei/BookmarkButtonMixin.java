package xin.vanilla.banira.internal.mixin.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.forge.compat.jei.JeiCompatibility;

/**
 * 捕获 JEI 书签按钮并为代理入口暴露原点击动作。
 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkButton", remap = false)
public abstract class BookmarkButtonMixin {
    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(IDrawable offIcon, IDrawable onIcon,
                                @Coerce Object overlay, @Coerce Object bookmarks,
                                @Coerce Object worldConfig, @Coerce Object keyBindings,
                                CallbackInfo callback) {
        JeiCompatibility.capture(this, offIcon, onIcon);
    }

    @Inject(method = "onMouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$blockNativeClick(@Coerce Object input,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }

}
