package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.fabric.client.FabricAuiClientHooks;

@Mixin(value = MouseHandler.class, priority = 2000)
public class AuiPointerInputMixin {
    @Inject(method = "onPress(JIII)V", at = @At("HEAD"))
    private void banira$observeButton(long window, int button, int action, int modifiers, CallbackInfo callback) {
        FabricAuiClientHooks.observeButton(window, button, action);
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"))
    private void banira$observeScroll(long window, double horizontal, double vertical, CallbackInfo callback) {
        FabricAuiClientHooks.observeScroll(window, horizontal, vertical);
    }
}
