package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.internal.fabric.client.FabricBaniraHudBridge;

/** Fabric 1.21.1 在两个独立原版方法上派发经验条与经验文本事件。 */
@Mixin(Gui.class)
public abstract class GuiExperienceMixin {
    @Unique
    private boolean banira$cancelExperienceBar;
    @Unique
    private boolean banira$cancelExperienceText;

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void banira$beforeExperience(GuiGraphics graphics, int x, CallbackInfo ci) {
        banira$cancelExperienceBar = FabricBaniraHudBridge.dispatchPre(HudOverlayElement.EXPERIENCE_BAR, graphics.pose());
        if (banira$cancelExperienceBar) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void banira$afterExperience(GuiGraphics graphics, int x, CallbackInfo ci) {
        if (!banira$cancelExperienceBar) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_BAR, graphics.pose());
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void banira$beforeExperienceText(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        banira$cancelExperienceText = FabricBaniraHudBridge.dispatchPre(HudOverlayElement.EXPERIENCE_TEXT, graphics.pose());
        if (banira$cancelExperienceText) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("RETURN"))
    private void banira$afterExperienceText(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!banira$cancelExperienceText) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_TEXT, graphics.pose());
        }
    }
}
