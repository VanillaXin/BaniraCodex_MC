package xin.vanilla.banira.internal.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * 原版客户端通知通道桥接：聊天、操作栏等 API 在不同 MC 版本变化较明显。
 */
public final class BaniraVanillaNotificationBridge {
    private BaniraVanillaNotificationBridge() {
    }

    public static boolean sendChat(@Nonnull net.minecraft.network.chat.Component message) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        player.sendMessage(message, player.getUUID());
        return true;
    }

    public static void sendActionBar(@Nonnull String line) {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.network.chat.Component barMsg = new TextComponent(line);
        UUID sender = mc.player != null ? mc.player.getUUID() : Util.NIL_UUID;
        BaniraClientRuntime.execute(() -> mc.gui.handleChat(ChatType.GAME_INFO, barMsg, sender));
    }

    public static boolean chatLinksEnabled() {
        return Minecraft.getInstance().options.chatLinks;
    }

    public static boolean runCommand(@Nonnull String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        String normalized = command.startsWith("/") ? command : "/" + command;
        player.chat(normalized);
        return true;
    }

    public static void suggestCommand(@Nonnull String command) {
        BaniraClientRuntime.setScreen(new ChatScreen(command));
    }
}
