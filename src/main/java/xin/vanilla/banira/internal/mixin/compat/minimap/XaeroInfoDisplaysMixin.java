package xin.vanilla.banira.internal.mixin.compat.minimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.fabric.compat.minimap.XaeroNotificationInfo;

import java.util.function.Consumer;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.info.BuiltInInfoDisplays", remap = false)
public abstract class XaeroInfoDisplaysMixin {
    @Inject(method = "forEach", at = @At("TAIL"), require = 0, remap = false)
    private static void banira$registerUnread(Consumer<Object> destination, CallbackInfo ci) {
        xin.vanilla.banira.internal.compat.OptionalIntegrationGuard.run("xaero-information", () -> XaeroNotificationInfo.append(destination));
    }
}
