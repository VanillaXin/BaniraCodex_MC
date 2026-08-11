package xin.vanilla.banira.internal.mixin.injections;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.client.gui.quickaction.QuickActionOverlay;

/** 防止创造模式选项卡穿透 Banira 快捷入口响应悬浮与点击。 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenQuickActionMixin {
    @Inject(method = "checkTabHovering", at = @At("HEAD"), cancellable = true)
    private void banira$suppressCoveredTabTooltip(PoseStack stack, CreativeModeTab group,
                                                   int mouseX, int mouseY,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (banira$captures(mouseX, mouseY)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void banira$suppressCoveredTabClick(double mouseX, double mouseY, int button,
                                                CallbackInfoReturnable<Boolean> callback) {
        if (banira$captures(mouseX, mouseY)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void banira$suppressCoveredTabRelease(double mouseX, double mouseY, int button,
                                                  CallbackInfoReturnable<Boolean> callback) {
        if (banira$captures(mouseX, mouseY)) {
            callback.setReturnValue(true);
        }
    }

    private boolean banira$captures(double mouseX, double mouseY) {
        return QuickActionOverlay.get().capturesPointer((Screen) (Object) this, mouseX, mouseY);
    }
}
