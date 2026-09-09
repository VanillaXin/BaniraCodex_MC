package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.internal.client.dev.BaniraNetworkSmokeClientRunner;

/** Observes system chat after native dispatch without replacing Fabric or vanilla handling. */
@Mixin(ClientPacketListener.class)
public class NetworkSmokeChatMixin {
    @Inject(method = "handleSystemChat", at = @At("RETURN"))
    private void banira$observeNativeNotification(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!packet.overlay()) BaniraNetworkSmokeClientRunner.receiveVanillaNotification(packet.content());
    }
}
