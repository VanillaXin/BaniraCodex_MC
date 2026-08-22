package xin.vanilla.banira.internal.mixin.compat.ftblibrary;

import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.fabric.compat.ftblibrary.FtbLibraryCompatibility;

/** 在 Banira 接管模式下关闭 FTB 原侧边栏的绘制与点击。 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton", remap = false)
public abstract class SidebarGroupGuiButtonMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private void banira$renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
                                     float partialTicks, CallbackInfo callback) {
        if (FtbLibraryCompatibility.shouldSuppressNativeGroup()) {
            FtbLibraryCompatibility.clearReservedArea();
            callback.cancel();
        }
    }

    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private void banira$onPress(CallbackInfo callback) {
        if (FtbLibraryCompatibility.shouldSuppressNativeGroup()) callback.cancel();
    }
}
