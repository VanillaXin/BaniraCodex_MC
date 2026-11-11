package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.internal.neoforge.client.NeoForgeHudOverlayAdapter;

/**
 * 在 NeoForge 分层提取中仅拦截真正的经验条和经验文本。
 */
@Mixin(Gui.class)
public abstract class GuiHudLayerMixin {
    @Redirect(method = "extractContextualInfoBarBackground",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void banira$experienceBar(ContextualBarRenderer renderer, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!(renderer instanceof ExperienceBarRenderer)) {
            renderer.extractBackground(graphics, deltaTracker);
        } else if (!NeoForgeHudOverlayAdapter.dispatchPre(HudOverlayElement.EXPERIENCE_BAR, graphics)) {
            renderer.extractBackground(graphics, deltaTracker);
            NeoForgeHudOverlayAdapter.dispatchPost(HudOverlayElement.EXPERIENCE_BAR, graphics);
        }
    }

    @Redirect(method = "extractExperienceLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    private void banira$experienceText(GuiGraphicsExtractor graphics, Font font, int level) {
        if (!NeoForgeHudOverlayAdapter.dispatchPre(HudOverlayElement.EXPERIENCE_TEXT, graphics)) {
            ContextualBarRenderer.extractExperienceLevel(graphics, font, level);
            NeoForgeHudOverlayAdapter.dispatchPost(HudOverlayElement.EXPERIENCE_TEXT, graphics);
        }
    }

}
