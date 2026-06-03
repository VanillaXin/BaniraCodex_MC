package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.platform.client.BaniraClientService;

import java.util.UUID;

/**
 * Forge 1.16.5 client player lookups. Client-only Minecraft access stays here.
 */
public final class ForgeBaniraClientService implements BaniraClientService {
    @Override
    public PlayerEntity localPlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public UUID localPlayerUuid() {
        PlayerEntity player = localPlayer();
        return player != null ? player.getUUID() : null;
    }

    @Override
    public String onlinePlayerName(UUID uuid) {
        if (uuid == null || Minecraft.getInstance().player == null || Minecraft.getInstance().player.connection == null) {
            return null;
        }
        return Minecraft.getInstance().player.connection.getOnlinePlayers().stream()
                .filter(info -> info.getProfile().getId().equals(uuid))
                .findFirst()
                .map(info -> info.getProfile().getName())
                .orElse(null);
    }

    @Override
    public PlayerEntity playerByUuid(UUID uuid) {
        if (uuid == null || Minecraft.getInstance().level == null) {
            return null;
        }
        return Minecraft.getInstance().level.getPlayerByUUID(uuid);
    }

    @Override
    public ResourceLocation playerSkin(UUID uuid) {
        if (uuid == null || Minecraft.getInstance().player == null || Minecraft.getInstance().player.connection == null) {
            return null;
        }
        return Minecraft.getInstance().player.connection.getOnlinePlayers().stream()
                .filter(info -> info.getProfile().getId().equals(uuid))
                .findFirst()
                .map(info -> info.getSkinLocation())
                .orElse(null);
    }

    @Override
    public String selectedLanguageCode() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
    }
}
