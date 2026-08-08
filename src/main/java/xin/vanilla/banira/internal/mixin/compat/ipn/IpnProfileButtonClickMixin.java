package xin.vanilla.banira.internal.mixin.compat.ipn;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.forge.compat.ipn.InventoryProfilesNextCompatibility;

/** 在接管模式下让已隐藏的 IPN 按钮不再占用鼠标命中区域。 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.base.ProfileButtonWidget", remap = false)
public abstract class IpnProfileButtonClickMixin {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$mouseClicked(int mouseX, int mouseY, int button,
                                     CallbackInfoReturnable<Boolean> callback) {
        if (InventoryProfilesNextCompatibility.shouldSuppressProfileButton(this)) {
            callback.setReturnValue(false);
        }
    }
}
