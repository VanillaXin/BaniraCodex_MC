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
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.BaniraCodex;

public final class MessageUtils {

    /**
     * 广播消息
     *
     * @param player  发送者
     * @param message 消息
     */
    public static void broadcastMessage(ServerPlayerEntity player, Component message) {
        player.server.getPlayerList().broadcastMessage(new TranslationTextComponent("chat.type.announcement", player.getDisplayName(), message.toChatComponent()), ChatType.SYSTEM, Util.NIL_UUID);
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastMessage(new TranslationTextComponent("chat.type.announcement", "Server", message.toChatComponent()), ChatType.SYSTEM, Util.NIL_UUID);
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
        player.sendMessage(message.toChatComponent(LanguageHelper.getPlayerLanguage(player)), player.getUUID());
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(PlayerEntity player, String message) {
        player.sendMessage(Component.literal(message).toChatComponent(), player.getUUID());
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(PlayerEntity player, String key, Object... args) {
        player.sendMessage(Component.translatable(key, args).languageCode(LanguageHelper.getPlayerLanguage(player)).toChatComponent(), player.getUUID());
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
            source.sendSuccess(Component.translatable(key, args).languageCode(LanguageHelper.getServerLanguage()).toChatComponent(), false);
        } else {
            source.sendFailure(Component.translatable(key, args).languageCode(LanguageHelper.getServerLanguage()).toChatComponent());
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
        player.connection.send(new SChatPacket(message.toChatComponent(LanguageHelper.getPlayerLanguage(player)), ChatType.GAME_INFO, player.getUUID()));
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
     * 发送数据包至服务器
     */
    public static <MSG> void sendPacketToServer(SimpleChannel channel, MSG msg) {
        channel.sendToServer(msg);
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG> void sendPacketToPlayer(SimpleChannel channel, MSG msg, ServerPlayerEntity player) {
        channel.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

}
