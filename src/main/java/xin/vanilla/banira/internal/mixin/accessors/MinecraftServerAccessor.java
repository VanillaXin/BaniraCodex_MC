package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 1.16 服务端资源容器没有公开 getter。
 */
@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
    @Accessor("resources")
    ServerResources banira$resources();
}
