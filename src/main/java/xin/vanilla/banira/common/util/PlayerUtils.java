package xin.vanilla.banira.common.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.internal.mixin.accessors.ServerPlayerAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class PlayerUtils {

    private PlayerUtils() {
    }

    public static UUID getPlayerUUID(@Nonnull PlayerEntity player) {
        return player.getUUID();
    }

    public static String getPlayerUUIDString(@Nonnull PlayerEntity player) {
        return player.getUUID().toString();
    }

    public static ITextComponent getPlayerName(PlayerEntity player) {
        return player == null
                ? Component.empty().toTextComponent()
                : player.getName();
    }

    @Nonnull
    public static String getPlayerNameString(PlayerEntity player) {
        return player == null
                ? ""
                : player.getName().getString();
    }

    @Nonnull
    public static ITextComponent getPlayerDisplayName(PlayerEntity player) {
        return player == null
                ? Component.empty().toTextComponent()
                : player.getDisplayName();
    }

    @Nonnull
    public static String getPlayerDisplayNameString(PlayerEntity player) {
        return player == null
                ? ""
                : player.getDisplayName().getString();
    }

    @Nullable
    public static ServerPlayerEntity getPlayerByUUID(String uuid) {
        return StringUtils.isNullOrEmptyEx(uuid)
                ? null
                : BaniraCodex.serverInstance().key().getPlayerList().getPlayer(UUID.fromString(uuid));
    }

    /**
     * 复制玩家客户端设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void cloneClientSettings(ServerPlayerEntity originalPlayer, ServerPlayerEntity targetPlayer) {
        ServerPlayerAccessor original = (ServerPlayerAccessor) originalPlayer;
        ServerPlayerAccessor target = (ServerPlayerAccessor) targetPlayer;

        target.language(original.language());
    }

}
