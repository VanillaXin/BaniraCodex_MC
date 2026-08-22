package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
    @Accessor("requestedViewDistance")
    void banira$setRequestedViewDistance(int viewDistance);

    @Accessor("chatVisibility")
    void banira$setChatVisibility(ChatVisiblity chatVisibility);

    @Accessor("canChatColor")
    void banira$setChatColors(boolean chatColors);

    @Accessor("textFilteringEnabled")
    void banira$setTextFilteringEnabled(boolean textFilteringEnabled);

    @Accessor("allowsListing")
    boolean banira$allowsListing();

    @Accessor("allowsListing")
    void banira$allowsListing(boolean allowsListing);
}
