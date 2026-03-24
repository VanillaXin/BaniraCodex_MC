package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public class MinecraftClientMixin {
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
