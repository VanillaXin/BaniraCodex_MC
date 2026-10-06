package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.client.notification.NotificationMapLineRenderer;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

@Pseudo
@Mixin(targets = "com.mamiyaotaru.voxelmap.Map", remap = false)
public abstract class VoxelMapNotificationMixin {
    @Shadow(remap = false)
    private int scHeight;
    @Shadow(remap = false)
    private int scWidth;
    @Shadow(remap = false)
    private boolean fullscreenMap;
    @Shadow(remap = false)
    @Final
    private MapSettingsManager options;
    @Shadow(remap = false)
    private String error;

    @Inject(method = "drawDirections", at = @At("TAIL"), require = 0, remap = false)
    private void banira$appendUnread(GuiGraphics graphics, int mapX, int mapY, CallbackInfo ci) {
        if (options.hide || fullscreenMap) return;
        int y = mapY > scHeight - 88 ? mapY - 45 - (options.coords ? 5 : 0)
                : mapY + 36 + (options.coords ? 10 : 0) + (error.isEmpty() ? 0 : 5);
        NotificationMapLineRenderer.drawCentered(graphics, NotificationMinimapBridge.text(EnumNotificationHudHost.VOXELMAP),
                mapX, y, 72, scWidth, scHeight);
    }
}
