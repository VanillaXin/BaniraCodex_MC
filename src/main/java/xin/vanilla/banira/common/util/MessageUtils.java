package xin.vanilla.banira.common.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.common.data.AbstractComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumNotificationVanillaFallback;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.packet.NotificationToClient;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.banira.internal.command.BaniraCommandAccess;
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.server.BaniraServerAccess;

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
        BaniraServerAccess
                .broadcastSystemMessage(null, new TranslatableComponent("chat.type.announcement", player.getDisplayName(), message.toChat()));
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        BaniraServerAccess
                .broadcastSystemMessage(server, new TranslatableComponent("chat.type.announcement", "Server", message.toChat()));
    }

    /**
     * 发送消息至所有玩家
     */
    public static void sendMessageToAll(Component message) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
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
        BaniraServerAccess.sendPlayerMessage(player, message.toChat(Translator.getPlayerLanguage(player)));
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(Player player, String message) {
        BaniraServerAccess.sendPlayerMessage(player, BaniraComponent.get().literal(message).toChat());
    }

    /**
     * 发送消息
     *
     * @param source  指令来源
     * @param success 是否成功
     */
    public static void sendMessage(CommandSourceStack source, boolean success, Component message) {
        if (BaniraCommandAccess.sourceEntity(source) instanceof ServerPlayer) {
            try {
                sendNotification(BaniraCommandAccess.sourcePlayer(source), message,
                        NotificationTypeKeys.COMMAND_FEEDBACK);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            BaniraCommandAccess.sendSuccess(source, message.languageCode(Translator.getServerLanguage()).toChat(), false);
        } else {
            BaniraCommandAccess.sendFailure(source, message.languageCode(Translator.getServerLanguage()).toChat());
        }
    }

    /**
     * 发送消息并且通知管理员
     *
     * @param source  指令来源
     * @param success 是否成功
     */
    public static void sendMessageWithAdmin(CommandSourceStack source, boolean success, Component message) {
        if (BaniraCommandAccess.sourceEntity(source) instanceof ServerPlayer) {
            try {
                sendNotification(BaniraCommandAccess.sourcePlayer(source), message,
                        NotificationTypeKeys.COMMAND_FEEDBACK);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            BaniraCommandAccess.sendSuccess(source, message.languageCode(Translator.getServerLanguage()).toChat(), true);
        } else {
            BaniraCommandAccess.sendFailure(source, message.languageCode(Translator.getServerLanguage()).toChat());
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
        BaniraServerAccess.sendPlayerMessage(player,
                BaniraComponent.get().trans(key, args).languageCode(Translator.getPlayerLanguage(player)).toChat());
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
        if (BaniraCommandAccess.sourceEntity(source) instanceof ServerPlayer) {
            try {
                sendTranslatableMessage(BaniraCommandAccess.sourcePlayer(source), key, args);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            BaniraCommandAccess.sendSuccess(source, BaniraComponent.get().trans(key, args).languageCode(Translator.getServerLanguage()).toChat(), false);
        } else {
            BaniraCommandAccess.sendFailure(source, BaniraComponent.get().trans(key, args).languageCode(Translator.getServerLanguage()).toChat());
        }
    }

    /**
     * 发送操作栏消息至所有玩家
     */
    public static void sendActionBarMessageToAll(Component message) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendActionBarMessage(player, message);
        }
    }

    /**
     * 发送操作栏消息
     */
    public static void sendActionBarMessage(ServerPlayer player, Component message) {
        BaniraServerAccess.sendActionBarMessage(player, message.toChat(Translator.getPlayerLanguage(player)));
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
        if (!PlayerUtils.isRemoteClientModInstalled(player, BaniraCodex.MODID)) {
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
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendNotification(player, component, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumNotificationStyle style, String notificationType) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendNotification(player, component, style, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback, String notificationType) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendNotification(player, component, style, vanillaFallback, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, String notificationType) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendNotification(player, component, position, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, String notificationType) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendNotification(player, component, position, animation, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, String notificationType) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendNotification(player, component, position, animation, durationTimeMs, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, String notificationType) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
            sendNotification(player, component, position, animation, durationTimeMs, style, notificationType);
        }
    }

    public static void broadcastNotification(Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback, String notificationType) {
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
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
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
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
        for (ServerPlayer player : PlayerUtils.getAllPlayers()) {
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
        copy.languageCodeIfEmpty(Translator.getPlayerLanguage(player));
        return copy;
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
