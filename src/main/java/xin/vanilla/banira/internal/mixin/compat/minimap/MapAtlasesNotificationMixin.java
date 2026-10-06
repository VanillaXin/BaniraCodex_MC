package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.dicemc.config.Config;
import xin.vanilla.banira.client.notification.NotificationMapLineRenderer;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

@Pseudo
@Mixin(targets = "pepjebs.dicemc.gui.HudEventHandler", remap = false)
public abstract class MapAtlasesNotificationMixin {
    @Inject(method = "renderMapHUDFromItemStack", require = 0, remap = false,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;popPose()V", shift = At.Shift.AFTER, remap = true))
    private static void banira$appendUnread(MatrixStack stack, ItemStack atlas, MapItemRenderer renderer, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int size = Config.FORCE_MINIMAP_SCALING.get();
        int top = mc.player.getActiveEffects().isEmpty() ? 0 : 26;
        NotificationMapLineRenderer.drawCentered(stack, NotificationMinimapBridge.text(EnumNotificationHudHost.MAP_ATLASES),
                mc.getWindow().getGuiScaledWidth() - size / 2f, top + size + 2, size);
    }
}
