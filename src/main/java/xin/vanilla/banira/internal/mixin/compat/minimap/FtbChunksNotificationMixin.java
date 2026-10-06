package xin.vanilla.banira.internal.mixin.compat.minimap;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.client.notification.NotificationMinimapBridge;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.FTBChunksClient", remap = false)
public abstract class FtbChunksNotificationMixin {
    @Inject(method = "buildMinimapTextData", at = @At("RETURN"), require = 0, remap = false)
    private void banira$appendUnread(CallbackInfoReturnable<List<Component>> ci) {
        List<Component> lines = ci.getReturnValue();
        if (lines == null) return;
        Component text = NotificationMinimapBridge.text(EnumNotificationHudHost.FTB_CHUNKS);
        if (!text.getString().isEmpty()) lines.add(text);
    }
}
