package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.ui.MapAtlasesHUD;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

@Pseudo
@Mixin(targets = "pepjebs.mapatlases.client.ui.MapAtlasesHUD", remap = false)
public abstract class MapAtlasesNotificationMixin {
    @Inject(method = "renderMapHUD", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT, require = 0, remap = false)
    private void banira$appendUnread(PoseStack stack, CallbackInfo ci, String mapId, MapItemSavedData data,
                                     int size, double bufferSize, int mapSize, float mapScale, String anchor,
                                     int x, int y, MultiBufferSource.BufferSource buffers, float textScale,
                                     int heightOffset, int widthOffset) {
        String text = NotificationMinimapBridge.text(EnumNotificationHudHost.MAP_ATLASES).getString();
        if (text.isEmpty()) return;
        boolean coords = MapAtlasesMod.CONFIG.drawMinimapCoords;
        boolean biome = MapAtlasesMod.CONFIG.drawMinimapBiome;
        int offset = anchor.contains("Lower")
                ? (coords || biome ? heightOffset - (coords ? (int) (12 * textScale) : 0) - (int) (12 * textScale)
                : -(int) (12 * textScale))
                : heightOffset + (biome ? (int) (12 * textScale) : 0);
        MapAtlasesHUD.drawScaledText(stack, x, y, text, textScale, widthOffset, offset);
    }
}
