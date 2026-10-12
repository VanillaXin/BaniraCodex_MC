package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.client.Minecraft;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;
import xin.vanilla.banira.internal.client.minimap.MinimapTextLayout;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.FTBChunksClient", remap = false)
public abstract class FtbChunksNotificationMixin {
    @Shadow(remap = false)
    @Final
    private static List<ITextComponent> MINIMAP_TEXT_LIST;

    @Inject(method = "renderHud", require = 0, remap = false,
            slice = @Slice(from = @At(value = "FIELD", target = "Ldev/ftb/mods/ftbchunks/client/FTBChunksClient;MINIMAP_TEXT_LIST:Ljava/util/List;", ordinal = 0)),
            at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0))
    private void banira$appendUnread(MatrixStack stack, float partialTicks, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        double mapScale = FTBChunksClientConfig.MINIMAP_SCALE.get() * 4 / mc.getWindow().getGuiScale();
        double textScale = mapScale / 2;
        int width = Math.max(1, (int) (Math.min((int) (64 * mapScale), mc.getWindow().getGuiScaledWidth()) / textScale));
        for (MinimapLineSnapshot row : NotificationMinimapBridge.rows(EnumNotificationHudHost.FTB_CHUNKS)) {
            MINIMAP_TEXT_LIST.addAll(MinimapTextLayout.wrap(row.text(), mc.font.getSplitter(), width));
        }
    }

    @ModifyArg(method = "renderHud", require = 0, remap = false, index = 1,
            slice = @Slice(from = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z")),
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/matrix/MatrixStack;translate(DDD)V", ordinal = 0, remap = true))
    private double banira$placeNativeText(double y) {
        Minecraft mc = Minecraft.getInstance();
        double mapScale = FTBChunksClientConfig.MINIMAP_SCALE.get() * 4 / mc.getWindow().getGuiScale();
        double textHeight = (Math.max(0, MINIMAP_TEXT_LIST.size() - 1) * 11 + mc.font.lineHeight + 1) * mapScale / 2;
        return MinimapTextLayout.nativeY(y, (int) (64 * mapScale), textHeight, mc.getWindow().getGuiScaledHeight());
    }
}
