package xin.vanilla.banira.internal.mixin.injections;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.BaniraHudSyntheticEvents;

@Mixin(Gui.class)
public class IngameGuiExperienceMixin {

    // MC 1.16.5 只有整段经验 HUD 绘制方法，因此在首个等级文本绘制前拆分事件。
    @Inject(
            method = "renderExperienceBar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I",
                    ordinal = 0
            ),
            cancellable = true
    )
    private void banira$beforeExperienceText(PoseStack stack, int x, CallbackInfo ci) {
        if (BaniraHudSyntheticEvents.beforeExperienceText(stack, x)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void banira$afterExperienceText(PoseStack stack, int x, CallbackInfo ci) {
        BaniraHudSyntheticEvents.afterExperienceText(stack, x);
    }
}
