package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.api.event.BaniraPlayerEvent;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.internal.fabric.event.ServerPlayerLogoutState;

import java.util.ArrayList;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void banira$beforePlayerLogout(ServerPlayer player, CallbackInfo ci) {
        banira$dispatchLogout(player);
    }

    // Shutdown can finish before asynchronous disconnect reaches native removal.
    @Inject(method = "removeAll()V", at = @At("HEAD"))
    private void banira$beforeRemoveAll(CallbackInfo ci) {
        for (ServerPlayer player : new ArrayList<>(((PlayerList) (Object) this).getPlayers())) {
            banira$dispatchLogout(player);
        }
    }

    @Unique
    private static void banira$dispatchLogout(ServerPlayer player) {
        if (!((ServerPlayerLogoutState) player).banira$beginLogout()) return;
        BaniraPlayerEvent event = new BaniraPlayerEvent(player, player.getUUID(), player.getName().getString());
        BaniraEventBus.dispatchPlayerSave(event);
        BaniraEventBus.dispatchPlayerLoggedOut(event);
    }
}
