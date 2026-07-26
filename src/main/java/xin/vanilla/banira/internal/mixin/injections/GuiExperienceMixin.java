package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.api.client.hud.HudOverlayElement;
import xin.vanilla.banira.internal.fabric.client.FabricBaniraHudBridge;

/**
 * Fabric 1.20.1 将经验条与经验文本拆成可独立取消的 Banira 事件。
 */
@Mixin(Gui.class)
public abstract class GuiExperienceMixin {
    @Unique
    private boolean banira$cancelExperienceBar;
    @Unique
    private boolean banira$cancelExperienceText;

    @Inject(method = "renderExperienceBar", at = @At("HEAD"))
    private void banira$beforeExperience(GuiGraphics graphics, int x, CallbackInfo ci) {
        banira$cancelExperienceBar = FabricBaniraHudBridge.dispatchPre(HudOverlayElement.EXPERIENCE_BAR, graphics.pose());
        banira$cancelExperienceText = FabricBaniraHudBridge.dispatchPre(HudOverlayElement.EXPERIENCE_TEXT, graphics.pose());
    }

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V")
    )
    private void banira$drawExperienceBar(GuiGraphics graphics, ResourceLocation texture,
                                          int x, int y, int u, int v, int width, int height) {
        if (!banira$cancelExperienceBar) {
            graphics.blit(texture, x, y, u, v, width, height);
        }
    }

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I")
    )
    private int banira$drawExperienceText(GuiGraphics graphics, Font font, String text,
                                          int x, int y, int color, boolean shadow) {
        return banira$cancelExperienceText ? 0 : graphics.drawString(font, text, x, y, color, shadow);
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void banira$afterExperience(GuiGraphics graphics, int x, CallbackInfo ci) {
        if (!banira$cancelExperienceBar) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_BAR, graphics.pose());
        }
        if (!banira$cancelExperienceText) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_TEXT, graphics.pose());
        }
    }
}
