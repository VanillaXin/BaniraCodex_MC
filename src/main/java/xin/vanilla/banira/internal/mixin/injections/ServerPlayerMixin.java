package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.client.CClientSettingsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.common.util.PlayerLanguageManager;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin {
    @Inject(
            method = "updateOptions",
            at = @At("TAIL")
    )
    private void banira$afterUpdateOptions(CClientSettingsPacket packet, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        PlayerLanguageManager.set(player, packet.getLanguage());
    }
}
