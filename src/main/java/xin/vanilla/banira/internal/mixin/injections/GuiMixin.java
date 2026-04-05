package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.client.util.NotificationManager;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("RETURN"))
    private void banira$afterHudRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        NotificationManager.get().render(guiGraphics);
    }
}
