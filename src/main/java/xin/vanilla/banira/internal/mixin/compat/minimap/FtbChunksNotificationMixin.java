package xin.vanilla.banira.internal.mixin.compat.minimap;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.FTBChunksClient", remap = false)
public abstract class FtbChunksNotificationMixin {
    @Shadow(remap = false)
    @Final
    private static List<Component> MINIMAP_TEXT_LIST;

    @Inject(method = "renderHud", require = 0, remap = false,
            slice = @Slice(from = @At(value = "FIELD", target = "Ldev/ftb/mods/ftbchunks/client/FTBChunksClient;MINIMAP_TEXT_LIST:Ljava/util/List;", ordinal = 0)),
            at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0))
    private void banira$appendUnread(PoseStack stack, float partialTicks, CallbackInfo ci) {
        Component text = NotificationMinimapBridge.text(EnumNotificationHudHost.FTB_CHUNKS);
        if (!text.getString().isEmpty()) MINIMAP_TEXT_LIST.add(text);
    }
}
