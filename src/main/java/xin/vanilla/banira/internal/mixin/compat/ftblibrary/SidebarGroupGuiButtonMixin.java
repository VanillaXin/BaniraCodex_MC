package xin.vanilla.banira.internal.mixin.compat.ftblibrary;

import com.mojang.blaze3d.matrix.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.forge.compat.ftblibrary.FtbLibraryCompatibility;

/** 在 Banira 接管模式下关闭 FTB 原侧边栏的绘制与点击。 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton", remap = false)
public abstract class SidebarGroupGuiButtonMixin {
    @Inject(method = {"render", "func_230430_a_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$render(MatrixStack stack, int mouseX, int mouseY,
                               float partialTicks, CallbackInfo callback) {
        FtbLibraryCompatibility.updateGroupWidgetVisibility(this);
        if (FtbLibraryCompatibility.shouldSuppressNativeGroup()) callback.cancel();
    }

    @Inject(method = {"onPress", "func_230930_b_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void banira$onPress(CallbackInfo callback) {
        if (FtbLibraryCompatibility.shouldSuppressNativeGroup()) callback.cancel();
    }
}
