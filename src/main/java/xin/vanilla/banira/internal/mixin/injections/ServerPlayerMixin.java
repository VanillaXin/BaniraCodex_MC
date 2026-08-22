package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.client.CClientSettingsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.common.util.PlayerOptionsManager;
import xin.vanilla.banira.common.util.PlayerUtils;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin {
    @Inject(
            method = "updateOptions",
            at = @At("TAIL"),
            require = 0
    )
    private void banira$afterUpdateOptions(CClientSettingsPacket packet, CallbackInfo ci) {
        try {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
            PlayerOptionsManager.set(player, packet.getLanguage(), packet.getChatVisibility(),
                    packet.getChatColors(), packet.getMainHand());
        } catch (Throwable ignored) {
        }
    }

    @Inject(
            method = "restoreFrom",
            at = @At("TAIL"),
            require = 0
    )
    private void banira$afterRestoreFrom(ServerPlayerEntity originalPlayer, boolean keepEverything, CallbackInfo ci) {
        try {
            PlayerUtils.cloneClientSettings(originalPlayer, (ServerPlayerEntity) (Object) this);
        } catch (Throwable ignored) {
        }
    }
}
