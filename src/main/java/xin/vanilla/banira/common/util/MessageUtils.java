package xin.vanilla.banira.common.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.*;
import xin.vanilla.banira.common.network.packet.NotificationToClient;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.internal.common.BaniraServerRuntime;
import xin.vanilla.banira.internal.config.CustomConfig;

public final class MessageUtils {
    private MessageUtils() {
    }


    /**
     * 广播消息
     *
     * @param player  发送者
     * @param message 消息
     */
    public static void broadcastMessage(ServerPlayer player, Component message) {
        player.server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.translatable("chat.type.announcement", player.getDisplayName(), message.toChat()), false);
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.translatable("chat.type.announcement", net.minecraft.network.chat.Component.literal("Server"), message.toChat()), false);
    }

    /**
     * 发送消息至所有玩家
     */
    public static void sendMessageToAll(Component message) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendMessage(player, message);
        }
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(Player player, Component message) {
        net.minecraft.network.chat.Component chat = message.toChat(Translator.getPlayerLanguage(player));
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(chat);
        } else {
            player.displayClientMessage(chat, false);
        }
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(Player player, String message) {
        net.minecraft.network.chat.Component chat = BaniraComponent.get().literal(message).toChat();
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(chat);
        } else {
            player.displayClientMessage(chat, false);
        }
    }

    /**
     * 发送消息
     *
     * @param source  指令来源
     * @param success 是否成功
     */
    public static void sendMessage(CommandSourceStack source, boolean success, Component message) {
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
            try {
                sendMessage(source.getPlayerOrException(), message);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            source.sendSuccess(message.languageCode(Translator.getServerLanguage()).toChat(), false);
        } else {
            source.sendFailure(message.languageCode(Translator.getServerLanguage()).toChat());
        }
    }

    /**
     * 发送消息并且通知管理员
     *
     * @param source  指令来源
     * @param success 是否成功
     */
    public static void sendMessageWithAdmin(CommandSourceStack source, boolean success, Component message) {
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
            try {
                sendMessage(source.getPlayerOrException(), message);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            source.sendSuccess(message.languageCode(Translator.getServerLanguage()).toChat(), true);
        } else {
            source.sendFailure(message.languageCode(Translator.getServerLanguage()).toChat());
        }
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(Player player, String key, Object... args) {
        net.minecraft.network.chat.Component chat = BaniraComponent.get().trans(key, args).languageCode(Translator.getPlayerLanguage(player)).toChat();
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(chat);
        } else {
            player.displayClientMessage(chat, false);
        }
    }

    /**
     * 发送翻译消息
     *
     * @param source  指令来源
     * @param success 是否成功
     * @param key     翻译键
     * @param args    参数
     */
    public static void sendTranslatableMessage(CommandSourceStack source, boolean success, String key, Object... args) {
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
            try {
                sendTranslatableMessage(source.getPlayerOrException(), key, args);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            source.sendSuccess(BaniraComponent.get().trans(key, args).languageCode(Translator.getServerLanguage()).toChat(), false);
        } else {
            source.sendFailure(BaniraComponent.get().trans(key, args).languageCode(Translator.getServerLanguage()).toChat());
        }
    }

    /**
     * 发送操作栏消息至所有玩家
     */
    public static void sendActionBarMessageToAll(Component message) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendActionBarMessage(player, message);
        }
    }

    /**
     * 发送操作栏消息
     */
    public static void sendActionBarMessage(ServerPlayer player, Component message) {
        player.displayClientMessage(message.toChat(Translator.getPlayerLanguage(player)), true);
    }

    // region 指定通知类型 — sendNotification / broadcastNotification

    /**
     * 向指定玩家发送通知（指定 {@code notificationType}，位置与动画取服务端该类型登记默认值，5s、NORMAL、聊天栏回退）
     */
    public static void sendNotification(ServerPlayer player, Component component, String notificationType) {
        sendNotification(player, component, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT, notificationType);
    }

    public static void sendNotification(ServerPlayer player, Component component, EnumNotificationStyle style, String notificationType) {
        String tid = NotificationTypeKeys.normalizeOrDefault(notificationType);
        sendNotification(player, component, ServerNotificationTypeRegistry.defaultPosition(tid), ServerNotificationTypeRegistry.defaultAnimation(tid), 5000L, style, EnumNotificationVanillaFallback.CHAT, tid);
    }

    public static void sendNotification(ServerPlayer player, Component component, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback, String notificationType) {
        String tid = NotificationTypeKeys.normalizeOrDefault(notificationType);
        sendNotification(player, component, ServerNotificationTypeRegistry.defaultPosition(tid), ServerNotificationTypeRegistry.defaultAnimation(tid), 5000L, style, vanillaFallback, tid);
    }

    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, String notificationType) {
        sendNotification(player, component, position, EnumMoveType.AUTO, 5000L, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT, notificationType);
    }

    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, String notificationType) {
        sendNotification(player, component, position, animation, 5000L, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT, notificationType);
    }

    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, String notificationType) {
        sendNotification(player, component, position, animation, durationTimeMs, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT, notificationType);
    }

    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, String notificationType) {
        sendNotification(player, component, position, animation, durationTimeMs, style, EnumNotificationVanillaFallback.CHAT, notificationType);
    }

    /**
     * 向指定玩家发送通知（完整参数，含 {@code notificationType}）
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback, String notificationType) {
        String tid = NotificationTypeKeys.normalizeOrDefault(notificationType);
        ServerNotificationTypeRegistry.ensureKnown(tid);
        Component payload = notificationPayloadForPlayer(player, component);
        if (!PlayerUtils.isRemoteClientModInstalled(player, Banira.MOD_ID)) {
            if (vanillaFallback == EnumNotificationVanillaFallback.ACTION_BAR) {
                sendActionBarMessage(player, payload);
            } else {
                sendMessage(player, payload);
            }
            return;
        }
        String uuid = PlayerUtils.getPlayerUUIDString(player);
        if (CustomConfig.notificationReceiveModeVanillaMessage.equals(CustomConfig.getPlayerNotificationReceiveMode(uuid))) {
            if (vanillaFallback == EnumNotificationVanillaFallback.ACTION_BAR) {
                sendActionBarMessage(player, payload);
            } else {
                sendMessage(player, payload);
            }
            return;
        }
        NotificationData data = NotificationData.of(payload, position, animation, durationTimeMs, style, tid);
        PacketUtils.sendPacketToPlayer(new NotificationToClient(data), player);
    }

    /**
     * 广播通知（指定类型，位置与动画取服务端默认值，5s、NORMAL、聊天栏回退）
     */
    public static void broadcastNotification(Component component, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumNotificationStyle style, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, style, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, style, vanillaFallback, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, position, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, position, animation, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, position, animation, durationTimeMs, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, position, animation, durationTimeMs, style, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback, String notificationType) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendNotification(player, component, position, animation, durationTimeMs, style, vanillaFallback, notificationType);
        }
    }

    // endregion

    // region 默认类型 {@link NotificationTypeKeys#DEFAULT} — sendDefaultNotification / broadcastDefaultNotification

    /**
     * 向指定玩家发送默认类型通知（位置与动画取服务端对 {@link NotificationTypeKeys#DEFAULT} 的登记，5s、NORMAL、聊天栏回退）
     */
    public static void sendDefaultNotification(ServerPlayer player, Component component) {
        sendDefaultNotification(player, component, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumNotificationVanillaFallback vanillaFallback) {
        sendDefaultNotification(player, component, EnumNotificationStyle.NORMAL, vanillaFallback);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumNotificationStyle style) {
        sendDefaultNotification(player, component, style, EnumNotificationVanillaFallback.CHAT);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback) {
        String tid = NotificationTypeKeys.DEFAULT;
        sendNotification(player, component, ServerNotificationTypeRegistry.defaultPosition(tid), ServerNotificationTypeRegistry.defaultAnimation(tid), 5000L, style, vanillaFallback, tid);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumPosition position) {
        sendDefaultNotification(player, component, position, EnumMoveType.AUTO, 5000L, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation) {
        sendDefaultNotification(player, component, position, animation, 5000L, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs) {
        sendDefaultNotification(player, component, position, animation, durationTimeMs, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style) {
        sendDefaultNotification(player, component, position, animation, durationTimeMs, style, EnumNotificationVanillaFallback.CHAT);
    }

    public static void sendDefaultNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback) {
        sendNotification(player, component, position, animation, durationTimeMs, style, vanillaFallback, NotificationTypeKeys.DEFAULT);
    }

    public static void broadcastDefaultNotification(Component component) {
        broadcastDefaultNotification(component, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void broadcastDefaultNotification(Component component, EnumNotificationVanillaFallback vanillaFallback) {
        broadcastDefaultNotification(component, EnumNotificationStyle.NORMAL, vanillaFallback);
    }

    public static void broadcastDefaultNotification(Component component, EnumNotificationStyle style) {
        broadcastDefaultNotification(component, style, EnumNotificationVanillaFallback.CHAT);
    }

    public static void broadcastDefaultNotification(Component component, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendDefaultNotification(player, component, style, vanillaFallback);
        }
    }

    public static void broadcastDefaultNotification(Component component, EnumPosition position) {
        broadcastDefaultNotification(component, position, EnumMoveType.AUTO, 5000L, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void broadcastDefaultNotification(Component component, EnumPosition position, EnumMoveType animation) {
        broadcastDefaultNotification(component, position, animation, 5000L, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void broadcastDefaultNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs) {
        broadcastDefaultNotification(component, position, animation, durationTimeMs, EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.CHAT);
    }

    public static void broadcastDefaultNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style) {
        broadcastDefaultNotification(component, position, animation, durationTimeMs, style, EnumNotificationVanillaFallback.CHAT);
    }

    public static void broadcastDefaultNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback) {
        for (ServerPlayer player : BaniraServerRuntime.players()) {
            sendDefaultNotification(player, component, position, animation, durationTimeMs, style, vanillaFallback);
        }
    }

    // endregion

    /**
     * 为发往指定玩家的通知克隆组件并绑定语言，保留子节点、换行与点击/悬停等结构（经 {@link AbstractComponent#serialize} 网络传输）
     */
    public static Component notificationPayloadForPlayer(ServerPlayer player, Component component) {
        if (component == null || component.isEmpty()) {
            return BaniraComponent.get().literal("");
        }
        Component copy = component.clone();
        String lang = Translator.getPlayerLanguage(player);
        copy.languageCodeIfEmpty(lang);
        if (requiresServerResolvedNotificationPayload(player, copy)) {
            return BaniraComponent.get().literal(copy.getString(lang, false, true));
        }
        return copy;
    }

    /**
     * 客户端可选子 Mod 的语言文件可能不存在；此时由服务端先按玩家语言解析为文本。
     */
    private static boolean requiresServerResolvedNotificationPayload(ServerPlayer player, Component component) {
        if (component == null) {
            return false;
        }
        if (component.i18nType() != EnumI18nType.PLAIN
                && component.i18nType() != EnumI18nType.NONE
                && component.i18nType() != EnumI18nType.ORIGINAL
                && !component.isModIdEmpty()
                && !canClientResolveTranslation(player, component.modId())) {
            return true;
        }
        for (Component child : component.getChildren()) {
            if (requiresServerResolvedNotificationPayload(player, child)) {
                return true;
            }
        }
        for (Component arg : component.getArgs()) {
            if (requiresServerResolvedNotificationPayload(player, arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canClientResolveTranslation(ServerPlayer player, String modId) {
        return "minecraft".equals(modId)
                || Banira.MOD_ID.equals(modId)
                || PlayerUtils.isRemoteClientModInstalled(player, modId);
    }

    /**
     * 将通知内容按目标玩家语言解析为纯文本 {@link AbstractComponent#literal}（会丢失换行结构、点击与悬停事件等）
     */
    public static Component literalComponent(ServerPlayer player, Component component) {
        if (component == null || component.isEmpty()) {
            return BaniraComponent.get().literal("");
        }
        String lang = Translator.getPlayerLanguage(player);
        String resolved = component.getString(lang, true, true);
        return BaniraComponent.get().literal(resolved != null ? resolved : "");
    }

}
