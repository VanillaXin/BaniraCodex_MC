package xin.vanilla.banira.internal.mixin.compat.minimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.fabric.compat.minimap.XaeroNotificationInfo;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.info.InfoDisplayManager", remap = false)
public abstract class XaeroInfoDisplayManagerMixin {
    @Inject(method = "applyLocalConfig", at = @At("RETURN"), require = 0, remap = false)
    private void banira$attachInformationRows(CallbackInfo ci) {
        xin.vanilla.banira.internal.compat.OptionalIntegrationGuard.run("xaero-information", () -> {
            XaeroNotificationInfo.attach(this);

        });
    }
}
