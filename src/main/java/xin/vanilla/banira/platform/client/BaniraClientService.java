package xin.vanilla.banira.platform.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import xin.vanilla.banira.common.data.KeyValue;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Loader/version-neutral access to client-only player state.
 */
public interface BaniraClientService {
    PlayerEntity localPlayer();

    UUID localPlayerUuid();

    String onlinePlayerName(UUID uuid);

    PlayerEntity playerByUuid(UUID uuid);

    ResourceLocation playerSkin(UUID uuid);

    /**
     * Builds vanilla item tooltip lines using the active client version's tooltip flag API.
     */
    List<ITextComponent> itemTooltip(ItemStack stack, PlayerEntity player, boolean advanced);

    String selectedLanguageCode();

    boolean hasResource(ResourceLocation location);

    void bindTexture(ResourceLocation location);

    long windowHandle();

    boolean isWindowActive();

    /**
     * Returns whether a client screen is currently open without exposing the screen implementation type.
     */
    boolean hasScreen();

    /**
     * Schedules work on the client thread when the loader requires it.
     */
    void runOnClientThread(Runnable action);

    /**
     * Reads the operating-system clipboard through the active client adapter.
     */
    String clipboard();

    /**
     * Writes clipboard text through the active client adapter.
     */
    void clipboard(String value);

    double guiScale();

    KeyValue<Integer, Integer> screenSize();

    KeyValue<Integer, Integer> guiScaledSize();

    KeyValue<Integer, Integer> guiPixelSize();

    /**
     * Draws text through the active client version's font/render-context API.
     */
    int drawText(Object nativeContext, String text, int x, int y, int argb, boolean shadow);

    int textWidth(String text);

    int lineHeight();

    static BaniraClientService noop() {
        return Noop.INSTANCE;
    }

    /**
     * Dedicated-server fallback; avoids loading client-only Minecraft classes.
     */
    final class Noop implements BaniraClientService {
        private static final Noop INSTANCE = new Noop();

        private Noop() {
        }

        @Override
        public PlayerEntity localPlayer() {
            return null;
        }

        @Override
        public UUID localPlayerUuid() {
            return null;
        }

        @Override
        public String onlinePlayerName(UUID uuid) {
            return null;
        }

        @Override
        public PlayerEntity playerByUuid(UUID uuid) {
            return null;
        }

        @Override
        public ResourceLocation playerSkin(UUID uuid) {
            return null;
        }

        @Override
        public List<ITextComponent> itemTooltip(ItemStack stack, PlayerEntity player, boolean advanced) {
            return stack != null ? Collections.singletonList(stack.getHoverName()) : Collections.emptyList();
        }

        @Override
        public String selectedLanguageCode() {
            return null;
        }

        @Override
        public boolean hasResource(ResourceLocation location) {
            return false;
        }

        @Override
        public void bindTexture(ResourceLocation location) {
        }

        @Override
        public long windowHandle() {
            return 0L;
        }

        @Override
        public boolean isWindowActive() {
            return false;
        }

        @Override
        public boolean hasScreen() {
            return false;
        }

        @Override
        public void runOnClientThread(Runnable action) {
            if (action != null) {
                action.run();
            }
        }

        @Override
        public String clipboard() {
            return "";
        }

        @Override
        public void clipboard(String value) {
        }

        @Override
        public double guiScale() {
            return 1.0D;
        }

        @Override
        public KeyValue<Integer, Integer> screenSize() {
            return new KeyValue<>(0, 0);
        }

        @Override
        public KeyValue<Integer, Integer> guiScaledSize() {
            return new KeyValue<>(0, 0);
        }

        @Override
        public KeyValue<Integer, Integer> guiPixelSize() {
            return new KeyValue<>(0, 0);
        }

        @Override
        public int drawText(Object nativeContext, String text, int x, int y, int argb, boolean shadow) {
            return x + textWidth(text);
        }

        @Override
        public int textWidth(String text) {
            return text != null ? text.length() * 6 : 0;
        }

        @Override
        public int lineHeight() {
            return 9;
        }
    }
}
