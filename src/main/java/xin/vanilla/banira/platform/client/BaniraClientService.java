package xin.vanilla.banira.platform.client;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

/**
 * Loader/version-neutral access to client-only player state.
 */
public interface BaniraClientService {
    UUID localPlayerUuid();

    String onlinePlayerName(UUID uuid);

    PlayerEntity playerByUuid(UUID uuid);

    ResourceLocation playerSkin(UUID uuid);

    String selectedLanguageCode();

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
        public String selectedLanguageCode() {
            return null;
        }
    }
}
