package xin.vanilla.banira.internal.mixin.injections;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.client.util.NotificationManager;

/** 在完整 HUD 帧结束后绘制一次无界面通知。 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;F)V", at = @At("RETURN"))
    private void banira$afterHudRender(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        NotificationManager.get().render(poseStack);
    }
}
