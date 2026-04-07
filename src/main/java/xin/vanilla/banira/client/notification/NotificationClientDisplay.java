package xin.vanilla.banira.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.Translator;

/**
 * 按玩家在「通知类型配置」中的选择，将网络通知改道至原版聊天或操作栏
 */
@OnlyIn(Dist.CLIENT)
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
            if (player == null) {
                return false;
            }
            player.displayClientMessage(component.toChat(lang), false);
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
            net.minecraft.network.chat.Component barMsg = net.minecraft.network.chat.Component.literal(line);
            Player player = mc.player;
            if (player == null) {
                return false;
            }
            mc.execute(() -> player.displayClientMessage(barMsg, true));
            return true;
        }
        return false;
    }
}
