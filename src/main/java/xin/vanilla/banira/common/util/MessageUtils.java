package xin.vanilla.banira.common.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TranslationTextComponent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.network.packet.NotificationToClient;
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
    public static void broadcastMessage(ServerPlayerEntity player, Component message) {
        player.server.getPlayerList().broadcastMessage(new TranslationTextComponent("chat.type.announcement", player.getDisplayName(), message.toChat()), ChatType.SYSTEM, Util.NIL_UUID);
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastMessage(new TranslationTextComponent("chat.type.announcement", "Server", message.toChat()), ChatType.SYSTEM, Util.NIL_UUID);
    }

    /**
     * 发送消息至所有玩家
     */
    public static void sendMessageToAll(Component message) {
        for (ServerPlayerEntity player : BaniraCodex.serverInstance().key().getPlayerList().getPlayers()) {
            sendMessage(player, message);
        }
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(PlayerEntity player, Component message) {
        player.sendMessage(message.toChat(Translator.getPlayerLanguage(player)), player.getUUID());
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(PlayerEntity player, String message) {
        player.sendMessage(Component.literal(message).toChat(), player.getUUID());
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(PlayerEntity player, String key, Object... args) {
        player.sendMessage(Component.trans(key, args).languageCode(Translator.getPlayerLanguage(player)).toChat(), player.getUUID());
    }

    /**
     * 发送翻译消息
     *
     * @param source  指令来源
     * @param success 是否成功
     * @param key     翻译键
     * @param args    参数
     */
    public static void sendTranslatableMessage(CommandSource source, boolean success, String key, Object... args) {
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
            try {
                sendTranslatableMessage(source.getPlayerOrException(), key, args);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            source.sendSuccess(Component.trans(key, args).languageCode(Translator.getServerLanguage()).toChat(), false);
        } else {
            source.sendFailure(Component.trans(key, args).languageCode(Translator.getServerLanguage()).toChat());
        }
    }

    /**
     * 发送操作栏消息至所有玩家
     */
    public static void sendActionBarMessageToAll(Component message) {
        for (ServerPlayerEntity player : BaniraCodex.serverInstance().key().getPlayerList().getPlayers()) {
            sendActionBarMessage(player, message);
        }
    }

    /**
     * 发送操作栏消息
     */
    public static void sendActionBarMessage(ServerPlayerEntity player, Component message) {
        player.connection.send(new SChatPacket(message.toChat(Translator.getPlayerLanguage(player)), ChatType.GAME_INFO, player.getUUID()));
    }

    /**
     * 广播数据包至所有玩家
     *
     * @param packet 数据包
     */
    public static void broadcastPacket(IPacket<?> packet) {
        BaniraCodex.serverInstance().key().getPlayerList().getPlayers().forEach(player -> player.connection.send(packet));
    }

    /**
     * 向指定玩家发送 Notification
     *
     * @param player    目标玩家
     * @param component 通知内容
     */
    public static void sendNotification(ServerPlayerEntity player, Component component) {
        sendNotification(player, component, EnumNotificationStyle.NORMAL);
    }

    /**
     * 向指定玩家发送 Notification
     */
    public static void sendNotification(ServerPlayerEntity player, Component component, EnumNotificationStyle style) {
        Component payload = literalComponent(player, component);
        NotificationData data = NotificationData.of(payload, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO, 5000L, style);
        PacketUtils.sendPacketToPlayer(NetworkInit.HANDLER.getChannel(), new NotificationToClient(data), player);
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
    public static void sendNotification(ServerPlayerEntity player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs) {
        sendNotification(player, component, position, animation, durationTimeMs, EnumNotificationStyle.NORMAL);
    }

    /**
     * 向指定玩家发送 Notification
     */
    public static void sendNotification(ServerPlayerEntity player, Component component, EnumPosition position, EnumMoveType animation, long durationTimeMs, EnumNotificationStyle style) {
        Component payload = literalComponent(player, component);
        NotificationData data = NotificationData.of(payload, position, animation, durationTimeMs, style);
        PacketUtils.sendPacketToPlayer(NetworkInit.HANDLER.getChannel(), new NotificationToClient(data), player);
    }

    /**
     * 将通知内容按目标玩家语言解析为纯文本 {@link Component#literal}
     */
    public static Component literalComponent(ServerPlayerEntity player, Component component) {
        if (component == null || component.isEmpty()) {
            return Component.literal("");
        }
        String lang = Translator.getPlayerLanguage(player);
        String resolved = component.getString(lang, true, true);
        return Component.literal(resolved != null ? resolved : "");
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
    public static void broadcastNotification(Component component, EnumNotificationStyle style) {
        for (ServerPlayerEntity player : BaniraCodex.serverInstance().key().getPlayerList().getPlayers()) {
            sendNotification(player, component, style);
        }
    }

}
