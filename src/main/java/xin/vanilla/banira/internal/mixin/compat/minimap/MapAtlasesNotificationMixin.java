package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import xin.vanilla.banira.client.notification.NotificationMapLineRenderer;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

@Pseudo
@Mixin(targets = "pepjebs.mapatlases.client.ui.MapAtlasesHUD", remap = false)
public abstract class MapAtlasesNotificationMixin {
    @Inject(method = "renderMapHUDFromItemStack", require = 0, remap = false,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", shift = At.Shift.AFTER, remap = true))
    private void banira$appendUnread(PoseStack stack, ItemStack atlas, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        int size = MapAtlasesMod.CONFIG == null ? 64 : MapAtlasesMod.CONFIG.forceMiniMapScaling;
        int top = mc.player.getActiveEffects().isEmpty() ? 0 : 26;
        NotificationMapLineRenderer.drawCentered(stack, NotificationMinimapBridge.text(EnumNotificationHudHost.MAP_ATLASES),
                mc.getWindow().getGuiScaledWidth() - size / 2f, top + size + 2, size);
    }
}
