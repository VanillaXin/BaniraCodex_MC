package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 访问死亡重建时原版未复制的服务端玩家选项。 */
@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerOptionsAccessor {
    @Accessor("chatVisibility")
    void banira$setChatVisibility(ChatVisibility chatVisibility);

    @Accessor("canChatColor")
    void banira$setChatColors(boolean chatColors);
}
