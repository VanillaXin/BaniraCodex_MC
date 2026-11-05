package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatAbilities;
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
            method = {"computeChatAbilities()Lnet/minecraft/client/multiplayer/chat/ChatAbilities;"},
            cancellable = true
    )
    public void banira$chat(CallbackInfoReturnable<ChatAbilities> callbackInfo) {
        callbackInfo.setReturnValue(ChatAbilities.NO_RESTRICTIONS);
        callbackInfo.cancel();
    }
}
