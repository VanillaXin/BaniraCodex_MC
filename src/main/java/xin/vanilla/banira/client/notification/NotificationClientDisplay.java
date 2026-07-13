package xin.vanilla.banira.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;

import net.minecraft.network.chat.TextComponent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.Translator;

import java.util.UUID;

/**
 * 按玩家在「通知类型配置」中的选择，将网络通知改道至原版聊天或操作栏
 */
public final class NotificationClientDisplay {

    private NotificationClientDisplay() {
    }

    /**
     * @return true 表示已用原版方式展示，不应再加入 Banira 浮层
     */
    public static boolean deliverVanillaIfConfigured(Component component, String typeId) {
        String key = NotificationTypeKeys.normalizeOrDefault(typeId);
        EnumNotificationTypeDisplayMode mode = NotificationTypeSettingsStore.get().getOrCreate(key).displayMode();
        if (mode == null || mode == EnumNotificationTypeDisplayMode.OVERLAY) {
            return false;
        }
        String lang = Translator.getClientLanguage();
        Minecraft mc = Minecraft.getInstance();
        if (mode == EnumNotificationTypeDisplayMode.VANILLA_CHAT) {
            Player player = mc.player;
            if (player != null) {
                player.sendMessage(component.toChat(lang), player.getUUID());
            }
            return true;
        }
        if (mode == EnumNotificationTypeDisplayMode.ACTION_BAR) {
            String line = component.getString(lang);
            if (line.isEmpty()) {
                line = component.toChat(lang).getString();
            }
            if (line.trim().isEmpty()) {
                return false;
            }
            net.minecraft.network.chat.Component barMsg = new TextComponent(line);
            UUID sender = mc.player != null ? mc.player.getUUID() : Util.NIL_UUID;
            mc.execute(() -> mc.gui.handleChat(ChatType.GAME_INFO, barMsg, sender));
            return true;
        }
        return false;
    }
}
