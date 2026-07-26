package xin.vanilla.banira.internal.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 客户端玩家状态实现；只能由 common 侧反射桥接调用。
 */
public final class BaniraClientPlayerRuntime {
    private BaniraClientPlayerRuntime() {
    }

    @Nullable
    public static Player localPlayer() {
        return Minecraft.getInstance().player;
    }

    @Nullable
    public static Player levelPlayer(@Nullable UUID uuid) {
        return uuid != null && Minecraft.getInstance().level != null
                ? Minecraft.getInstance().level.getPlayerByUUID(uuid)
                : null;
    }

    @Nullable
    public static String onlinePlayerName(@Nullable UUID uuid) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null || uuid == null) return null;
        return player.connection.getOnlinePlayers().stream()
                .filter(info -> info.getProfile().getId().equals(uuid))
                .findFirst()
                .map(info -> info.getProfile().getName())
                .orElse(null);
    }

    @Nullable
    public static ResourceLocation onlinePlayerSkin(@Nullable UUID uuid) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null || uuid == null) return null;
        return player.connection.getOnlinePlayers().stream()
                .filter(info -> info.getProfile().getId().equals(uuid))
                .findFirst()
                .map(info -> info.getSkinLocation())
                .orElse(null);
    }
}
