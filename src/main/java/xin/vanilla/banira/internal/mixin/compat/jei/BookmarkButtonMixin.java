package xin.vanilla.banira.internal.mixin.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.config.IClientToggleState;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.forge.compat.jei.JeiCompatibility;

/** 捕获 JEI 1.21.1 书签控制器并阻止被接管后的原生点击。 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.overlay.bookmarks.BookmarkButtonController", remap = false)
public abstract class BookmarkButtonMixin {
    @Shadow @Final private IDrawable offIcon;
    @Shadow @Final private IDrawable onIcon;
    @Shadow @Final private IClientToggleState toggleState;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void banira$capture(BookmarkOverlay overlay, BookmarkList bookmarks,
                                IClientToggleState toggleState, IInternalKeyMappings keyBindings,
                                CallbackInfo callback) {
        JeiCompatibility.capture(this, offIcon, onIcon, this.toggleState);
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$blockNativeClick(mezz.jei.api.gui.inputs.IJeiUserInput input,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldBlockNativeClick(this)) callback.setReturnValue(false);
    }

}
