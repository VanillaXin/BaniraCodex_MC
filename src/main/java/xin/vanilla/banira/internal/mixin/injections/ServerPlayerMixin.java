package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.common.util.PlayerOptionsManager;
import xin.vanilla.banira.common.util.PlayerUtils;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(
            method = "updateOptions",
            at = @At("TAIL"),
            require = 1
    )
    private void banira$afterUpdateOptions(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PlayerOptionsManager.set(player, packet.language(), player.getChatVisibility(),
                player.canChatInColor(), player.getMainArm(), player.allowsListing());
        // Rebuild both the status and Forge's serialized status cache on the next tick.
        player.server.invalidateStatus();
    }

    @Inject(
            method = "restoreFrom",
            at = @At("TAIL"),
            require = 1
    )
    private void banira$afterRestoreFrom(ServerPlayer originalPlayer, boolean keepEverything, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PlayerUtils.cloneClientSettings(originalPlayer, player);
    }
}
