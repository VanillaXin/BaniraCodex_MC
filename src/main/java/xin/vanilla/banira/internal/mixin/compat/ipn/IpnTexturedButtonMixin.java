package xin.vanilla.banira.internal.mixin.compat.ipn;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.client.compat.ipn.IpnCompatibility;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.base.TexturedButtonWidget", remap = false)
public abstract class IpnTexturedButtonMixin {
    @Inject(method = {"mouseClicked(III)Z", "mouseScrolled(IIDD)Z"}, at = @At("HEAD"), cancellable = true)
    private void banira$ignoreHostedButtonInput(CallbackInfoReturnable<Boolean> callback) {
        if (IpnCompatibility.suppress(this)) callback.setReturnValue(false);
    }
}
