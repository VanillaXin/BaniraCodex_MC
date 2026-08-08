package xin.vanilla.banira.internal.mixin.compat.ipn;

import org.anti_ad.mc.common.gui.NativeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.forge.compat.ipn.InventoryProfilesNextCompatibility;

/** 在接管模式下屏蔽 IPN 设置与编辑器按钮的原生绘制。 */
@Pseudo
@Mixin(targets = "org.anti_ad.mc.ipnext.gui.inject.base.SortButtonWidget", remap = false)
public abstract class IpnButtonMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$render(NativeContext context, int mouseX, int mouseY,
                               float partialTicks, CallbackInfo callback) {
        if (InventoryProfilesNextCompatibility.shouldSuppressProfileButton(this)) callback.cancel();
    }
}
