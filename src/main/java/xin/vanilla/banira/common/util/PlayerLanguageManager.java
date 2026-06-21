package xin.vanilla.banira.common.util;

import xin.vanilla.banira.internal.server.ServerSenderAccess;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerLanguageManager {
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final Map<UUID, String> languageMap = new ConcurrentHashMap<>();

    private PlayerLanguageManager() {
    }

    public static boolean has(Object player) {
        return ServerSenderAccess.uuid(player) != null;
    }

    public static String get(Object player) {
        UUID uuid = ServerSenderAccess.uuid(player);
        return uuid != null ? get(uuid) : DEFAULT_LANGUAGE;
    }

    public static String get(UUID uuid) {
        return uuid != null ? languageMap.getOrDefault(uuid, DEFAULT_LANGUAGE) : DEFAULT_LANGUAGE;
    }

    public static void set(Object player, String language) {
        UUID uuid = ServerSenderAccess.uuid(player);
        if (uuid != null) {
            set(uuid, language);
        }
    }

    public static void set(UUID uuid, String language) {
        if (uuid != null && language != null) {
            languageMap.put(uuid, language);
        }
    }

    public static void remove(Object player) {
        UUID uuid = ServerSenderAccess.uuid(player);
        if (uuid != null) {
            remove(uuid);
        }
    }

    public static void remove(UUID uuid) {
        if (uuid != null) {
            languageMap.remove(uuid);
        }
    }

    public static void clear() {
        languageMap.clear();
    }
}
