package xin.vanilla.banira.common.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
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
import xin.vanilla.banira.internal.config.CustomConfig;
import xin.vanilla.banira.internal.network.NetworkInit;

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
        for (ServerPlayer player : BaniraCodex.serverInstance().key().getPlayerList().getPlayers()) {
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
        for (ServerPlayer player : BaniraCodex.serverInstance().key().getPlayerList().getPlayers()) {
            sendActionBarMessage(player, message);
        }
    }

    /**
     * 发送操作栏消息
     */
    public static void sendActionBarMessage(ServerPlayer player, Component message) {
        player.displayClientMessage(message.toChat(Translator.getPlayerLanguage(player)), true);
    }

    /**
     * 向指定玩家发送 Notification
     *
     * @param player    目标玩家
     * @param component 通知内容
     */
    public static void sendNotification(ServerPlayer player, Component component) {
        sendNotification(player, component, EnumNotificationStyle.NORMAL);
    }

    /**
     * 向指定玩家发送 Notification
     *
     * @param vanillaFallback 对端无 Banira 客户端时的回退展示（默认聊天栏）
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumNotificationVanillaFallback vanillaFallback) {
        sendNotification(player, component, EnumNotificationStyle.NORMAL, vanillaFallback);
    }

    /**
     * 向指定玩家发送 Notification
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumNotificationStyle style) {
        sendNotification(player, component, style, EnumNotificationVanillaFallback.CHAT);
    }

    /**
     * 向指定玩家发送 Notification
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback) {
        sendNotification(player, component, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, 5000L, style, vanillaFallback);
    }

    /**
     * 向指定玩家发送 Notification
     *
     * @param player         目标玩家
     * @param component      通知内容
     * @param position       位置
     * @param animation      动画
     * @param durationTimeMs 持续时间（毫秒）
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs) {
        sendNotification(player, component, position, animation, durationTimeMs, EnumNotificationStyle.NORMAL);
    }

    /**
     * 向指定玩家发送 Notification
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationVanillaFallback vanillaFallback) {
        sendNotification(player, component, position, animation, durationTimeMs, EnumNotificationStyle.NORMAL, vanillaFallback);
    }

    /**
     * 向指定玩家发送 Notification
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style) {
        sendNotification(player, component, position, animation, durationTimeMs, style, EnumNotificationVanillaFallback.CHAT);
    }

    /**
     * 向指定玩家发送 Notification
     */
    public static void sendNotification(ServerPlayer player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback) {
        Component payload = literalComponent(player, component);
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
        NotificationData data = NotificationData.of(payload, position, animation, durationTimeMs, style);
        PacketUtils.sendPacketToPlayer(NetworkInit.HANDLER.getChannel(), new NotificationToClient(data), player);
    }

    /**
     * 将通知内容按目标玩家语言解析为纯文本 {@link AbstractComponent#literal}
     */
    public static Component literalComponent(ServerPlayer player, Component component) {
        if (component == null || component.isEmpty()) {
            return BaniraComponent.get().literal("");
        }
        String lang = Translator.getPlayerLanguage(player);
        String resolved = component.getString(lang, true, true);
        return BaniraComponent.get().literal(resolved != null ? resolved : "");
    }

    /**
     * 向所有在线玩家广播 Notification
     */
    public static void broadcastNotification(Component component) {
        broadcastNotification(component, EnumNotificationStyle.NORMAL);
    }

    /**
     * 向所有在线玩家广播 Notification
     */
    public static void broadcastNotification(Component component, EnumNotificationVanillaFallback vanillaFallback) {
        broadcastNotification(component, EnumNotificationStyle.NORMAL, vanillaFallback);
    }

    /**
     * 向所有在线玩家广播 Notification
     */
    public static void broadcastNotification(Component component, EnumNotificationStyle style) {
        broadcastNotification(component, style, EnumNotificationVanillaFallback.CHAT);
    }

    /**
     * 向所有在线玩家广播 Notification
     */
    public static void broadcastNotification(Component component, EnumNotificationStyle style, EnumNotificationVanillaFallback vanillaFallback) {
        for (ServerPlayer player : BaniraCodex.serverInstance().key().getPlayerList().getPlayers()) {
            sendNotification(player, component, style, vanillaFallback);
        }
    }

}
