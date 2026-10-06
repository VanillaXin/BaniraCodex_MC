package xin.vanilla.banira.internal.mixin.compat.minimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import xin.vanilla.banira.client.notification.NotificationMapLineRenderer;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

@Pseudo
@Mixin(targets = "pepjebs.mapatlases.client.ui.MapAtlasesHUD", remap = false)
public abstract class MapAtlasesNotificationMixin {
    @Shadow(remap = false)
    private float globalScale;

    @Inject(method = "renderText", at = @At("TAIL"), require = 0, remap = false)
    private void banira$appendUnread(GuiGraphics graphics, int x, int y, Anchoring anchor, CallbackInfo ci) {
        int rows = (MapAtlasesClientConfig.drawMinimapCoords.get() ? 1 : 0)
                + (MapAtlasesClientConfig.drawMinimapChunkCoords.get() ? 1 : 0)
                + (MapAtlasesClientConfig.drawMinimapBiome.get() ? 1 : 0);
        float textScale = MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get().floatValue();
        float lineY = anchor.isUp ? (y + 64) * globalScale + 2 + rows * 10 * textScale
                : y * globalScale - (rows == 0 ? 2 : 20 * globalScale * textScale + 2 * globalScale - 2) - 10 * textScale;
        // Native renderText returns inside the outer minimap scale.
        graphics.pose().pushPose();
        try {
            graphics.pose().scale(1 / globalScale, 1 / globalScale, 1);
            Minecraft mc = Minecraft.getInstance();
            NotificationMapLineRenderer.drawCentered(graphics,
                    NotificationMinimapBridge.text(EnumNotificationHudHost.MAP_ATLASES),
                    (x + 32) * globalScale, lineY, 64 * globalScale,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        } finally {
            graphics.pose().popPose();
        }
    }
}
