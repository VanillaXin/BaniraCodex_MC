package xin.vanilla.banira.internal.mixin.injections;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xin.vanilla.banira.common.event.BaniraGenericTeleportEvent;
import xin.vanilla.banira.common.util.BaniraEventBus;

/**
 * 非 {@link net.minecraft.entity.player.ServerPlayerEntity} 子类使用 {@link Entity#teleportTo(double, double, double)} 时派发传送回调。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private double banira$tpFromX;
    @Unique
    private double banira$tpFromY;
    @Unique
    private double banira$tpFromZ;

    @Inject(method = "teleportTo", at = @At("HEAD"))
    private void banira$tpHead(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        World level = self.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        banira$tpFromX = self.getX();
        banira$tpFromY = self.getY();
        banira$tpFromZ = self.getZ();
    }

    @Inject(method = "teleportTo", at = @At("TAIL"))
    private void banira$tpTail(double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        World level = self.level;
        if (level == null || level.isClientSide()) {
            return;
        }
        BaniraEventBus.EntityEvents.dispatchEntityTeleport(
                new BaniraGenericTeleportEvent(self, banira$tpFromX, banira$tpFromY, banira$tpFromZ, x, y, z, null, null, null)
        );
    }
}
