package xin.vanilla.banira.internal.mixin.compat.minimap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.compat.minimap.FtbNotificationInfo;
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.FTBChunksClient", remap = false)
public abstract class FtbChunksNotificationMixin {
    @Inject(method = "setupComponents", at = @At("HEAD"), require = 0, remap = false)
    private void banira$registerUnread(CallbackInfo ci) { FtbNotificationInfo.register(); }
}
