package xin.vanilla.banira.internal.mixin.compat.ipn;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.compat.ipn.IpnCompatibility;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.base.SortButtonWidget", remap = false)
public abstract class IpnSortButtonMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void banira$hideHostedButton(CallbackInfo callback) {
        if (IpnCompatibility.suppress(this)) callback.cancel();
    }
}
