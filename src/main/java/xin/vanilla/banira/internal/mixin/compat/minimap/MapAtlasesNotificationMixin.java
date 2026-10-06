package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pepjebs.mapatlases.client.Anchoring;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.utils.MapDataHolder;
import xin.vanilla.banira.client.notification.NotificationMapLineRenderer;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

@Pseudo
@Mixin(targets = "pepjebs.mapatlases.client.ui.MapAtlasesHUD", remap = false)
public abstract class MapAtlasesNotificationMixin {
    @Shadow(remap = false)
    private float globalScale;

    @Inject(method = "render", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT, require = 0, remap = false)
    private void banira$appendUnread(GuiGraphics graphics, float partialTicks, int width, int height, CallbackInfo ci,
                                     ItemStack atlas, MapDataHolder data, ClientLevel level, LocalPlayer player,
                                     PoseStack stack, int size, Anchoring anchor, int margin, int x, int y) {
        int rows = (MapAtlasesClientConfig.drawMinimapCoords.get() ? 1 : 0)
                + (MapAtlasesClientConfig.drawMinimapChunkCoords.get() ? 1 : 0)
                + (MapAtlasesClientConfig.drawMinimapBiome.get() ? 1 : 0);
        float textScale = MapAtlasesClientConfig.minimapCoordsAndBiomeScale.get().floatValue();
        float lineY = anchor.isUp ? (y + 64) * globalScale + 2 + rows * 10 * textScale
                : y * globalScale - Math.max(20, rows * 10) * textScale - 2 - 10 * textScale;
        NotificationMapLineRenderer.drawCentered(graphics,
                NotificationMinimapBridge.text(EnumNotificationHudHost.MAP_ATLASES),
                (x + 32) * globalScale, lineY, 64 * globalScale, width, height);
    }
}
