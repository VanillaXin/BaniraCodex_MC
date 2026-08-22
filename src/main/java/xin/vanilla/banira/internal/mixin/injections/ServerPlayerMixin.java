package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.common.util.PlayerOptionsManager;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.internal.mixin.accessors.ClientInformationPacketAccessor;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(
            method = "updateOptions",
            at = @At("TAIL"),
            require = 0
    )
    private void banira$afterUpdateOptions(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        try {
            ServerPlayer player = (ServerPlayer) (Object) this;
            PlayerOptionsManager.set(player,
                    ((ClientInformationPacketAccessor) packet).banira$language(),
                    packet.getChatVisibility(), packet.getChatColors(), packet.getMainHand());
        } catch (Throwable ignored) {
        }
    }

    @Inject(
            method = "restoreFrom",
            at = @At("TAIL"),
            require = 0
    )
    private void banira$afterRestoreFrom(ServerPlayer originalPlayer, boolean keepEverything, CallbackInfo ci) {
        try {
            PlayerUtils.cloneClientSettings(originalPlayer, (ServerPlayer) (Object) this);
        } catch (Throwable ignored) {
        }
    }
}
