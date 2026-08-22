package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 访问死亡重建时原版未复制的服务端玩家选项。 */
@Mixin(ServerPlayer.class)
public interface ServerPlayerOptionsAccessor {
    @Accessor("chatVisibility")
    void banira$setChatVisibility(ChatVisiblity chatVisibility);

    @Accessor("canChatColor")
    void banira$setChatColors(boolean chatColors);
}
