package xin.vanilla.banira.internal.mixin.accessors;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {
    @Accessor(value = "language")
    String banira$language();

    @Accessor(value = "language")
    void banira$language(String language);

    @Accessor("chatVisibility")
    void banira$setChatVisibility(ChatVisiblity chatVisibility);

    @Accessor("canChatColor")
    void banira$setChatColors(boolean chatColors);

    @Accessor("allowsListing")
    boolean banira$allowsListing();

    @Accessor("allowsListing")
    void banira$allowsListing(boolean allowsListing);
}
