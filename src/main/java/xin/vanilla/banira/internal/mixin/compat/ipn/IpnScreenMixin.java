package xin.vanilla.banira.internal.mixin.compat.ipn;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.compat.ipn.IpnCompatibility;

@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.ContainerScreenEventHandler", remap = false)
public abstract class IpnScreenMixin {
    @Inject(method = "preRender()V", at = @At("RETURN"), require = 0)
    private void banira$refreshButtons(CallbackInfo callback) {
        IpnCompatibility.update();
    }
}
