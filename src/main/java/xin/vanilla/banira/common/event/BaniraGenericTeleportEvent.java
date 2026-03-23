package xin.vanilla.banira.common.event;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

/**
 * 通用实体传送信息
 */
@Getter
@Accessors(chain = true, fluent = true)
public final class BaniraGenericTeleportEvent {
    private final Entity entity;
    private final double fromX;
    private final double fromY;
    private final double fromZ;
    private final double toX;
    private final double toY;
    private final double toZ;
    @Nullable
    private final ServerWorld targetWorld;
    @Nullable
    private final Float yaw;
    @Nullable
    private final Float pitch;

    public BaniraGenericTeleportEvent(
            Entity entity,
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ,
            @Nullable ServerWorld targetWorld,
            @Nullable Float yaw,
            @Nullable Float pitch
    ) {
        this.entity = entity;
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.toX = toX;
        this.toY = toY;
        this.toZ = toZ;
        this.targetWorld = targetWorld;
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
