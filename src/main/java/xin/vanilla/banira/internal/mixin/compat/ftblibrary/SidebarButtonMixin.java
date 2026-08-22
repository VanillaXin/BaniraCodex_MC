package xin.vanilla.banira.internal.mixin.compat.ftblibrary;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.fabric.compat.ftblibrary.FtbLibraryCompatibility;

/** 把 Banira 动态 FTB 按钮的点击转回其原始快捷操作。 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftblibrary.sidebar.RegisteredSidebarButton", remap = false)
public abstract class SidebarButtonMixin {
    @Inject(method = "getLangKey", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void banira$getLangKey(CallbackInfoReturnable<String> callback) {
        String key = FtbLibraryCompatibility.hostedButtonTranslationKey(this);
        if (key != null) callback.setReturnValue(key);
    }

    @Inject(method = "clickButton", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void banira$clickButton(boolean shiftDown, CallbackInfo callback) {
        if (FtbLibraryCompatibility.activateHostedButton(this, shiftDown)) callback.cancel();
    }
}
