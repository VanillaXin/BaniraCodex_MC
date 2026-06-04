package xin.vanilla.banira.internal.mixin.injections;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IngameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.BaniraHudSyntheticEvents;

@Mixin(IngameGui.class)
public class IngameGuiExperienceMixin {

    // Forge 1.16.5 only exposes a coarse EXPERIENCE overlay, so split the text draw locally.
    @Inject(
            method = "renderExperienceBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;draw(Lcom/mojang/blaze3d/matrix/MatrixStack;Ljava/lang/String;FFI)I",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void banira$beforeExperienceText(MatrixStack stack, int x, CallbackInfo ci) {
        if (BaniraHudSyntheticEvents.beforeExperienceText(stack)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void banira$afterExperienceText(MatrixStack stack, int x, CallbackInfo ci) {
        BaniraHudSyntheticEvents.afterExperienceText(stack);
    }
}
