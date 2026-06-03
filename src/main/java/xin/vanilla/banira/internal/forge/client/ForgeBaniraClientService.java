package xin.vanilla.banira.internal.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import xin.vanilla.banira.common.data.KeyValue;
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

    @Override
    public boolean hasResource(ResourceLocation location) {
        return location != null && Minecraft.getInstance().getResourceManager().hasResource(location);
    }

    @Override
    public void bindTexture(ResourceLocation location) {
        if (location != null) {
            Minecraft.getInstance().getTextureManager().bind(location);
        }
    }

    @Override
    public long windowHandle() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    @Override
    public boolean isWindowActive() {
        return Minecraft.getInstance().isWindowActive();
    }

    @Override
    public boolean hasScreen() {
        return Minecraft.getInstance().screen != null;
    }

    @Override
    public void runOnClientThread(Runnable action) {
        if (action != null) {
            Minecraft.getInstance().execute(action);
        }
    }

    @Override
    public String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void clipboard(String value) {
        Minecraft.getInstance().keyboardHandler.setClipboard(value != null ? value : "");
    }

    @Override
    public double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public KeyValue<Integer, Integer> screenSize() {
        if (Minecraft.getInstance().screen != null) {
            return new KeyValue<>(Minecraft.getInstance().screen.width, Minecraft.getInstance().screen.height);
        }
        return guiScaledSize();
    }

    @Override
    public KeyValue<Integer, Integer> guiScaledSize() {
        return new KeyValue<>(
                Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight()
        );
    }

    @Override
    public KeyValue<Integer, Integer> guiPixelSize() {
        return new KeyValue<>(
                Minecraft.getInstance().getWindow().getWidth(),
                Minecraft.getInstance().getWindow().getHeight()
        );
    }
}
