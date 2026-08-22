package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.server.level.ClientInformation;
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
            require = 0
    )
    private void banira$afterUpdateOptions(ClientInformation information, CallbackInfo ci) {
        try {
            ServerPlayer player = (ServerPlayer) (Object) this;
            PlayerOptionsManager.set(player, information.language(), information.viewDistance(),
                    information.chatVisibility(), information.chatColors(), information.mainHand(),
                    information.textFilteringEnabled(), information.allowsListing());
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
            ServerPlayer player = (ServerPlayer) (Object) this;
            PlayerUtils.cloneClientSettings(originalPlayer, player);
        } catch (Throwable ignored) {
        }
    }
}
