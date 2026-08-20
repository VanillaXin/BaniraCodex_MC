package xin.vanilla.banira.common.util;

import lombok.experimental.Accessors;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Accessors(chain = true, fluent = true)
public final class PlayerOptionsManager {
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final boolean DEFAULT_ALLOWS_LISTING = true;
    private static final Map<UUID, PlayerOptions> playerOptionsMap = new ConcurrentHashMap<>();

    private PlayerOptionsManager() {
    }

    public static String getLanguage(ServerPlayer player) {
        return getLanguage(player.getUUID());
    }

    public static String getLanguage(UUID uuid) {
        return getOptions(uuid).language();
    }

    public static void setLanguage(ServerPlayer player, String language) {
        setLanguage(player.getUUID(), language);
    }

    public static void setLanguage(UUID uuid, String language) {
        playerOptionsMap.compute(uuid, (key, options) -> new PlayerOptions(
                language,
                options != null ? options.allowsListing() : DEFAULT_ALLOWS_LISTING
        ));
    }

    public static boolean getAllowsListing(ServerPlayer player) {
        return getAllowsListing(player.getUUID());
    }

    public static boolean getAllowsListing(UUID uuid) {
        return getOptions(uuid).allowsListing();
    }

    public static void setAllowsListing(ServerPlayer player, boolean allowsListing) {
        setAllowsListing(player.getUUID(), allowsListing);
    }

    public static void setAllowsListing(UUID uuid, boolean allowsListing) {
        playerOptionsMap.compute(uuid, (key, options) -> new PlayerOptions(
                options != null ? options.language() : DEFAULT_LANGUAGE,
                allowsListing
        ));
    }

    public static void set(ServerPlayer player, String language, boolean allowsListing) {
        set(player.getUUID(), language, allowsListing);
    }

    public static void set(UUID uuid, String language, boolean allowsListing) {
        playerOptionsMap.put(uuid, new PlayerOptions(language, allowsListing));
    }

    public static void remove(ServerPlayer player) {
        remove(player.getUUID());
    }

    public static void remove(UUID uuid) {
        playerOptionsMap.remove(uuid);
    }

    public static void clear() {
        playerOptionsMap.clear();
    }

    private static PlayerOptions getOptions(UUID uuid) {
        return playerOptionsMap.getOrDefault(
                uuid,
                new PlayerOptions(DEFAULT_LANGUAGE, DEFAULT_ALLOWS_LISTING)
        );
    }

    private record PlayerOptions(String language, boolean allowsListing) {
    }
}
