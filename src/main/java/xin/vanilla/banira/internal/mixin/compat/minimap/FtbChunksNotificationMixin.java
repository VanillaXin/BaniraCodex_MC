package xin.vanilla.banira.internal.mixin.compat.minimap;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;
import xin.vanilla.banira.internal.client.minimap.MinimapTextLayout;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.FTBChunksClient", remap = false)
public abstract class FtbChunksNotificationMixin {
    @Inject(method = "buildMinimapTextData", at = @At("RETURN"), require = 0, remap = false)
    private void banira$appendUnread(CallbackInfoReturnable<List<Component>> ci) {
        List<Component> lines = ci.getReturnValue();
        if (lines == null) return;
        Minecraft mc = Minecraft.getInstance();
        float mapScale = (float) (FTBChunksClientConfig.MINIMAP_SCALE.get() * 4 / mc.getWindow().getGuiScale());
        if (FTBChunksClientConfig.MINIMAP_PROPORTIONAL.get()) {
            mapScale = (float) (mc.getWindow().getGuiScaledWidth() / 10F / 64 * FTBChunksClientConfig.MINIMAP_SCALE.get());
        }
        float textScale = Math.max(1F, Math.round(mapScale)) / 2F;
        int width = Math.max(1, (int) (Math.min((int) (64 * mapScale), mc.getWindow().getGuiScaledWidth()) / textScale));
        for (MinimapLineSnapshot row : NotificationMinimapBridge.rows(EnumNotificationHudHost.FTB_CHUNKS)) {
            lines.addAll(MinimapTextLayout.wrap(row.text(), mc.font.getSplitter(), width));
        }
    }
}
