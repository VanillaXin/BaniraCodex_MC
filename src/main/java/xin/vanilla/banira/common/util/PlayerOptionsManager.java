package xin.vanilla.banira.common.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
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
    private static final int DEFAULT_VIEW_DISTANCE = 2;
    private static final ChatVisiblity DEFAULT_CHAT_VISIBILITY = ChatVisiblity.FULL;
    private static final boolean DEFAULT_CHAT_COLORS = true;
    private static final HumanoidArm DEFAULT_MAIN_HAND = HumanoidArm.RIGHT;
    private static final boolean DEFAULT_TEXT_FILTERING = false;
    private static final boolean DEFAULT_ALLOWS_LISTING = false;
    private static final Map<UUID, PlayerOptions> playerOptionsMap = new ConcurrentHashMap<>();

    private PlayerOptionsManager() {
    }

    public static boolean has(Object player) {
        UUID uuid = ServerSenderAccess.uuid(player);
        return uuid != null && has(uuid);
    }

    public static boolean has(UUID uuid) {
        return uuid != null && playerOptionsMap.containsKey(uuid);
    }

    public static String getLanguage(Object player) {
        UUID uuid = ServerSenderAccess.uuid(player);
        return uuid != null ? getLanguage(uuid) : DEFAULT_LANGUAGE;
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

    public static void setLanguage(Object player, String language) {
        UUID uuid = ServerSenderAccess.uuid(player);
        if (uuid != null) setLanguage(uuid, language);
    }

    public static void setLanguage(UUID uuid, String language) {
        playerOptionsMap.compute(uuid, (key, options) -> new PlayerOptions(
                language,
                options != null ? options.viewDistance() : DEFAULT_VIEW_DISTANCE,
                options != null ? options.chatVisibility() : DEFAULT_CHAT_VISIBILITY,
                options == null || options.chatColors(),
                options != null ? options.mainHand() : DEFAULT_MAIN_HAND,
                options != null ? options.textFilteringEnabled() : DEFAULT_TEXT_FILTERING,
                options != null ? options.allowsListing() : DEFAULT_ALLOWS_LISTING
        ));
    }

    public static ChatVisiblity getChatVisibility(UUID uuid) {
        return getOptions(uuid).chatVisibility();
    }

    public static int getViewDistance(UUID uuid) {
        return getOptions(uuid).viewDistance();
    }

    public static boolean getChatColors(UUID uuid) {
        return getOptions(uuid).chatColors();
    }

    public static HumanoidArm getMainHand(UUID uuid) {
        return getOptions(uuid).mainHand();
    }

    public static boolean isTextFilteringEnabled(UUID uuid) {
        return getOptions(uuid).textFilteringEnabled();
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
                options != null ? options.viewDistance() : DEFAULT_VIEW_DISTANCE,
                options != null ? options.chatVisibility() : DEFAULT_CHAT_VISIBILITY,
                options == null || options.chatColors(),
                options != null ? options.mainHand() : DEFAULT_MAIN_HAND,
                options != null ? options.textFilteringEnabled() : DEFAULT_TEXT_FILTERING,
                allowsListing
        ));
    }

    public static void set(ServerPlayer player, String language, int viewDistance,
                           ChatVisiblity chatVisibility, boolean chatColors,
                           HumanoidArm mainHand, boolean textFilteringEnabled,
                           boolean allowsListing) {
        set(player.getUUID(), language, viewDistance, chatVisibility, chatColors,
                mainHand, textFilteringEnabled, allowsListing);
    }

    public static void set(UUID uuid, String language, int viewDistance,
                           ChatVisiblity chatVisibility, boolean chatColors,
                           HumanoidArm mainHand, boolean textFilteringEnabled,
                           boolean allowsListing) {
        playerOptionsMap.put(uuid, new PlayerOptions(
                language == null ? DEFAULT_LANGUAGE : language,
                viewDistance,
                chatVisibility == null ? DEFAULT_CHAT_VISIBILITY : chatVisibility,
                chatColors,
                mainHand == null ? DEFAULT_MAIN_HAND : mainHand,
                textFilteringEnabled,
                allowsListing));
    }

    public static void remove(ServerPlayer player) {
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
                new PlayerOptions(DEFAULT_LANGUAGE, DEFAULT_VIEW_DISTANCE,
                        DEFAULT_CHAT_VISIBILITY, DEFAULT_CHAT_COLORS, DEFAULT_MAIN_HAND,
                        DEFAULT_TEXT_FILTERING, DEFAULT_ALLOWS_LISTING)
        );
    }

    private record PlayerOptions(String language, int viewDistance,
                                 ChatVisiblity chatVisibility, boolean chatColors,
                                 HumanoidArm mainHand, boolean textFilteringEnabled,
                                 boolean allowsListing) {
    }
}
