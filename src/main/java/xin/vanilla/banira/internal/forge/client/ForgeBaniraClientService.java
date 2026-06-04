package xin.vanilla.banira.internal.forge.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.platform.client.BaniraClientService;

import java.util.Collections;
import java.util.List;
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
    public List<ITextComponent> itemTooltip(ItemStack stack, PlayerEntity player, boolean advanced) {
        if (stack == null) {
            return Collections.emptyList();
        }
        return stack.getTooltipLines(
                player,
                advanced ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL
        );
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

    @Override
    public int drawText(Object nativeContext, String text, int x, int y, int argb, boolean shadow) {
        if (text == null) {
            return x;
        }
        MatrixStack stack = matrixStack(nativeContext);
        if (shadow) {
            return Minecraft.getInstance().font.drawShadow(stack, text, x, y, argb);
        }
        return Minecraft.getInstance().font.draw(stack, text, x, y, argb);
    }

    @Override
    public int textWidth(String text) {
        return text != null ? Minecraft.getInstance().font.width(text) : 0;
    }

    @Override
    public int lineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public void fill(Object nativeContext, int x, int y, int width, int height, int argb) {
        AbstractGuiUtils.fill(matrixStack(nativeContext), x, y, width, height, argb);
    }

    @Override
    public void blit(Object nativeContext, ResourceLocation texture, int x, int y, double u, double v, int width, int height, int textureWidth, int textureHeight) {
        AbstractGuiUtils.blit(matrixStack(nativeContext), texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    private static MatrixStack matrixStack(Object nativeContext) {
        if (nativeContext instanceof MatrixStack) {
            return (MatrixStack) nativeContext;
        }
        throw new IllegalStateException("Unsupported draw context: " + nativeContext);
    }
}
