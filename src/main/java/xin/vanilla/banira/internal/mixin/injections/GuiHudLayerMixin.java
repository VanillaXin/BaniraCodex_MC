package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.internal.neoforge.client.NeoForgeHudOverlayAdapter;

/** NeoForge 1.21.1 不再提供逐 HUD 事件，这里恢复经验条与经验文本的可取消语义。 */
@Mixin(Gui.class)
public abstract class GuiHudLayerMixin {
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void banira$experienceBarPre(GuiGraphics graphics, int left, CallbackInfo callback) {
        if (NeoForgeHudOverlayAdapter.dispatchPre(HudOverlayElement.EXPERIENCE_BAR, graphics)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void banira$experienceBarPost(GuiGraphics graphics, int left, CallbackInfo callback) {
        NeoForgeHudOverlayAdapter.dispatchPost(HudOverlayElement.EXPERIENCE_BAR, graphics);
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void banira$experienceTextPre(GuiGraphics graphics, DeltaTracker deltaTracker,
                                          CallbackInfo callback) {
        if (NeoForgeHudOverlayAdapter.dispatchPre(HudOverlayElement.EXPERIENCE_TEXT, graphics)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("RETURN"))
    private void banira$experienceTextPost(GuiGraphics graphics, DeltaTracker deltaTracker,
                                           CallbackInfo callback) {
        NeoForgeHudOverlayAdapter.dispatchPost(HudOverlayElement.EXPERIENCE_TEXT, graphics);
    }
}
