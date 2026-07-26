package xin.vanilla.banira.internal.mixin.injections;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.internal.fabric.client.FabricBaniraHudBridge;

/**
 * Fabric 1.18.2 将经验条与经验文本拆成可独立取消的 Banira 事件。
 */
@Mixin(Gui.class)
public abstract class GuiExperienceMixin {
    @Unique
    private boolean banira$cancelExperienceBar;
    @Unique
    private boolean banira$cancelExperienceText;

    @Inject(method = "renderExperienceBar", at = @At("HEAD"))
    private void banira$beforeExperience(PoseStack stack, int x, CallbackInfo ci) {
        banira$cancelExperienceBar = FabricBaniraHudBridge.dispatchPre(HudOverlayElement.EXPERIENCE_BAR, stack);
        banira$cancelExperienceText = FabricBaniraHudBridge.dispatchPre(HudOverlayElement.EXPERIENCE_TEXT, stack);
    }

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V")
    )
    private void banira$drawExperienceBar(Gui gui, PoseStack stack, int x, int y,
                                          int u, int v, int width, int height) {
        if (!banira$cancelExperienceBar) {
            gui.blit(stack, x, y, u, v, width, height);
        }
    }

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;draw(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I")
    )
    private int banira$drawExperienceText(Font font, PoseStack stack, String text,
                                          float x, float y, int color) {
        return banira$cancelExperienceText ? 0 : font.draw(stack, text, x, y, color);
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void banira$afterExperience(PoseStack stack, int x, CallbackInfo ci) {
        if (!banira$cancelExperienceBar) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_BAR, stack);
        }
        if (!banira$cancelExperienceText) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_TEXT, stack);
        }
    }
}
