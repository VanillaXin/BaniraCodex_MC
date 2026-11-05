package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.dev.SmokeScreenshotCapture;

@Mixin(GameRenderer.class)
public abstract class SmokeScreenshotFrameMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void banira$afterFrame(CallbackInfo callback) {
        SmokeScreenshotCapture.afterFrame();
    }
}
