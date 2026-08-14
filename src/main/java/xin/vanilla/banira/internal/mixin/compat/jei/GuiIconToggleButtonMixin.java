package xin.vanilla.banira.internal.mixin.compat.jei;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.forge.compat.jei.JeiCompatibility;

/** 只屏蔽被接管的 BookmarkButton，不影响 JEI 其他图标按钮。 */
@Pseudo
@Mixin(targets = "mezz.jei.gui.elements.GuiIconToggleButton", remap = false)
public abstract class GuiIconToggleButtonMixin {
    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$draw(GuiGraphics graphics, int mouseX, int mouseY,
                             float partialTicks, CallbackInfo callback) {
        if (JeiCompatibility.shouldSuppress(this)) callback.cancel();
    }

    @Inject(method = "drawTooltips", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$drawTooltips(GuiGraphics graphics, int mouseX, int mouseY,
                                     CallbackInfo callback) {
        if (JeiCompatibility.shouldSuppress(this)) callback.cancel();
    }

    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$isMouseOver(double mouseX, double mouseY,
                                    CallbackInfoReturnable<Boolean> callback) {
        if (JeiCompatibility.shouldSuppress(this)) callback.setReturnValue(false);
    }
}
