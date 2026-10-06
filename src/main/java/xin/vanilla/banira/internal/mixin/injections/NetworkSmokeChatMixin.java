package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.dev.BaniraNetworkSmokeClientRunner;

import java.util.UUID;

/**
 * Observes vanilla delivery without replacing Fabric or vanilla chat dispatch.
 */
@Mixin(Gui.class)
public class NetworkSmokeChatMixin {
    @Inject(method = "handleChat", at = @At("RETURN"))
    private void banira$observeNativeNotification(ChatType type, Component message, UUID sender, CallbackInfo ci) {
        BaniraNetworkSmokeClientRunner.receiveVanillaNotification(message);
    }
}
