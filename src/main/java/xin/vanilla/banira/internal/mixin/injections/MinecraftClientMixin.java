package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xin.vanilla.banira.internal.client.BaniraClientEventHub;
import xin.vanilla.banira.common.util.AdvancementUtils;
import xin.vanilla.banira.common.util.PlayerUtils;

@Mixin({Minecraft.class})
public class MinecraftClientMixin {
    // World teardown runs on the client thread, even when the task queue is discarded.
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private void banira$beforePlayerLogout(CallbackInfo callbackInfo) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.player != null) {
            BaniraClientEventHub.dispatchClientPlayerLoggedOut(client.player);
            AdvancementUtils.clearAdvancementData();
            PlayerUtils.removeRemoteServerDataStatus(client.player);
        }
    }

    @Inject(
            at = {@At("HEAD")},
            method = {"allowsMultiplayer"},
            cancellable = true
    )
    public void banira$multiplayer(CallbackInfoReturnable<Boolean> callbackInfo) {
        callbackInfo.setReturnValue(true);
        callbackInfo.cancel();
    }

    @Inject(
            at = {@At("HEAD")},
            method = {"getChatStatus"},
            cancellable = true
    )
    public void banira$chat(CallbackInfoReturnable<Minecraft.ChatStatus> callbackInfo) {
        callbackInfo.setReturnValue(Minecraft.ChatStatus.ENABLED);
        callbackInfo.cancel();
    }
}
