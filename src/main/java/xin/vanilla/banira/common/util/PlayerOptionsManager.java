package xin.vanilla.banira.common.util;

import net.minecraft.entity.player.ChatVisibility;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.HandSide;
import xin.vanilla.banira.internal.server.ServerSenderAccess;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存客户端上报但原版死亡克隆未完整复制的玩家选项。
 * 这里只补齐 Clone 缺口，不承担通用玩家数据持久化。
 */
public final class PlayerOptionsManager {
    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final ChatVisibility DEFAULT_CHAT_VISIBILITY = ChatVisibility.FULL;
    private static final boolean DEFAULT_CHAT_COLORS = true;
    private static final HandSide DEFAULT_MAIN_HAND = HandSide.RIGHT;
    private static final Map<UUID, PlayerOptions> playerOptionsMap = new ConcurrentHashMap<>();

    private PlayerOptionsManager() {
    }

    public static boolean has(Object player) {
        return ServerSenderAccess.uuid(player) != null;
    }

    public static String getLanguage(Object player) {
        UUID uuid = ServerSenderAccess.uuid(player);
        return uuid != null ? getLanguage(uuid) : DEFAULT_LANGUAGE;
    }

    public static String getLanguage(ServerPlayerEntity player) {
        return getLanguage(player.getUUID());
    }

    public static String getLanguage(UUID uuid) {
        return getOptions(uuid).language();
    }

    public static void setLanguage(ServerPlayerEntity player, String language) {
        setLanguage(player.getUUID(), language);
    }

    public static void setLanguage(Object player, String language) {
        UUID uuid = ServerSenderAccess.uuid(player);
        if (uuid != null) setLanguage(uuid, language);
    }

    public static void setLanguage(UUID uuid, String language) {
        playerOptionsMap.compute(uuid, (key, options) -> new PlayerOptions(
                language,
                options != null ? options.chatVisibility() : DEFAULT_CHAT_VISIBILITY,
                options == null || options.chatColors(),
                options != null ? options.mainHand() : DEFAULT_MAIN_HAND
        ));
    }

    public static ChatVisibility getChatVisibility(UUID uuid) {
        return getOptions(uuid).chatVisibility();
    }

    public static boolean getChatColors(UUID uuid) {
        return getOptions(uuid).chatColors();
    }

    public static HandSide getMainHand(UUID uuid) {
        return getOptions(uuid).mainHand();
    }

    public static void set(ServerPlayerEntity player, String language, ChatVisibility chatVisibility,
                           boolean chatColors, HandSide mainHand) {
        set(player.getUUID(), language, chatVisibility, chatColors, mainHand);
    }

    public static void set(UUID uuid, String language, ChatVisibility chatVisibility,
                           boolean chatColors, HandSide mainHand) {
        playerOptionsMap.put(uuid, new PlayerOptions(
                language == null ? DEFAULT_LANGUAGE : language,
                chatVisibility == null ? DEFAULT_CHAT_VISIBILITY : chatVisibility,
                chatColors,
                mainHand == null ? DEFAULT_MAIN_HAND : mainHand));
    }

    public static void remove(ServerPlayerEntity player) {
        remove(player.getUUID());
    }

    public static void remove(Object player) {
        UUID uuid = ServerSenderAccess.uuid(player);
        if (uuid != null) remove(uuid);
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
                new PlayerOptions(DEFAULT_LANGUAGE, DEFAULT_CHAT_VISIBILITY,
                        DEFAULT_CHAT_COLORS, DEFAULT_MAIN_HAND)
        );
    }

    private static final class PlayerOptions {
        private final String language;
        private final ChatVisibility chatVisibility;
        private final boolean chatColors;
        private final HandSide mainHand;

        private PlayerOptions(String language, ChatVisibility chatVisibility,
                              boolean chatColors, HandSide mainHand) {
            this.language = language;
            this.chatVisibility = chatVisibility;
            this.chatColors = chatColors;
            this.mainHand = mainHand;
        }

        private String language() {
            return language;
        }

        private ChatVisibility chatVisibility() {
            return chatVisibility;
        }

        private boolean chatColors() {
            return chatColors;
        }

        private HandSide mainHand() {
            return mainHand;
        }
    }
}
