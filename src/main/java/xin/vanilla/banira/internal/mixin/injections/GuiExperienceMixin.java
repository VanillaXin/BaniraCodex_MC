package xin.vanilla.banira.internal.mixin.injections;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @WrapMethod(method = "renderExperienceBar")
    private void banira$renderExperience(GuiGraphics graphics, int x, Operation<Void> original) {
        banira$cancelExperienceBar = FabricBaniraHudBridge.dispatchPre(HudOverlayElement.EXPERIENCE_BAR, graphics.pose());
        banira$cancelExperienceText = FabricBaniraHudBridge.dispatchPre(
                HudOverlayElement.EXPERIENCE_TEXT, graphics.pose());
        original.call(graphics, x);

        if (!banira$cancelExperienceBar) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_BAR, graphics.pose());
        }
        if (!banira$cancelExperienceText) {
            FabricBaniraHudBridge.dispatchPost(HudOverlayElement.EXPERIENCE_TEXT, graphics.pose());
        }
    }

    /** 只改变本次渲染读取值，避免把 HUD 兼容逻辑写回玩家状态。 */
    @ModifyExpressionValue(
            method = "renderExperienceBar",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;experienceLevel:I"),
            require = 0
    )
    private int banira$experienceLevelForRendering(int original) {
        return banira$cancelExperienceText ? 0 : original;
    }

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),
            require = 0
    )
    private void banira$drawExperienceBar(GuiGraphics graphics, ResourceLocation texture,
                                          int x, int y, int u, int v, int width, int height) {
        if (!banira$cancelExperienceBar) {
            graphics.blit(texture, x, y, u, v, width, height);
        }
    }

}
