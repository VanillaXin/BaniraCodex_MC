package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.client.CClientSettingsPacket;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.common.event.BaniraGenericTeleportEvent;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.PlayerLanguageManager;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin {
    @Unique
    private boolean banira$sixArgTeleport;
    @Unique
    private double banira$tpFromX;
    @Unique
    private double banira$tpFromY;
    @Unique
    private double banira$tpFromZ;

    @Inject(
            method = "updateOptions",
            at = @At("TAIL")
    )
    private void banira$afterUpdateOptions(CClientSettingsPacket packet, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        PlayerLanguageManager.set(player, packet.getLanguage());
    }

    @Inject(
            method = "teleportTo(Lnet/minecraft/world/server/ServerWorld;DDDFF)V",
            at = @At("HEAD")
    )
    private void banira$tp6Head(ServerWorld world, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (world == null || world.isClientSide()) {
            return;
        }
        banira$sixArgTeleport = true;
        banira$tpFromX = self.getX();
        banira$tpFromY = self.getY();
        banira$tpFromZ = self.getZ();
    }

    @Inject(
            method = "teleportTo(Lnet/minecraft/world/server/ServerWorld;DDDFF)V",
            at = @At("TAIL")
    )
    private void banira$tp6Tail(ServerWorld world, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (world == null || world.isClientSide()) {
            return;
        }
        banira$sixArgTeleport = false;
        BaniraEventBus.EntityEvents.dispatchEntityTeleport(
                new BaniraGenericTeleportEvent(self, banira$tpFromX, banira$tpFromY, banira$tpFromZ, x, y, z, world, yaw, pitch)
        );
    }

    @Inject(
            method = "teleportTo(DDD)V",
            at = @At("HEAD")
    )
    private void banira$tp3Head(double x, double y, double z, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (self.level == null || self.level.isClientSide()) {
            return;
        }
        if (!banira$sixArgTeleport) {
            banira$tpFromX = self.getX();
            banira$tpFromY = self.getY();
            banira$tpFromZ = self.getZ();
        }
    }

    @Inject(
            method = "teleportTo(DDD)V",
            at = @At("TAIL")
    )
    private void banira$tp3Tail(double x, double y, double z, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        if (self.level == null || self.level.isClientSide()) {
            return;
        }
        if (banira$sixArgTeleport) {
            return;
        }
        BaniraEventBus.EntityEvents.dispatchEntityTeleport(
                new BaniraGenericTeleportEvent(self, banira$tpFromX, banira$tpFromY, banira$tpFromZ, x, y, z, null, null, null)
        );
    }
}
