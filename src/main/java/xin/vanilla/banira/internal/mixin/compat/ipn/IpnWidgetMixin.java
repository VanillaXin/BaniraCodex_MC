package xin.vanilla.banira.internal.mixin.compat.ipn;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.client.compat.ipn.IpnCompatibility;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.common.gui.widgets.Widget", remap = false)
public abstract class IpnWidgetMixin {
    @Inject(method = "mouseScrolled(IID)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$ignoreHostedButtonScroll(CallbackInfoReturnable<Boolean> callback) {
        if (IpnCompatibility.suppress(this)) callback.setReturnValue(false);
    }
}
