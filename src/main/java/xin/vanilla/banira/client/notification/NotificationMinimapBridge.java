package xin.vanilla.banira.client.notification;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.config.ClientConfig;

/** Optional map callbacks share the existing HUD visibility state. */
public final class NotificationMinimapBridge {
    private static final NotificationHudHostState HOSTS = new NotificationHudHostState();

    private NotificationMinimapBridge() {
    }

    public static EnumNotificationHudHost selectedHost() {
        return HOSTS.select(ClientConfig.get().notificationHud().host(), now(),
                Minecraft.getInstance().screen instanceof ChatScreen);
    }

    public static boolean claim(EnumNotificationHudHost host) {
        if (!NotificationUnreadHud.isVisible() || Minecraft.getInstance().screen instanceof ChatScreen) return false;
        long now = now();
        HOSTS.seen(host, now);
        return HOSTS.select(ClientConfig.get().notificationHud().host(), now) == host;
    }

    public static Component text(EnumNotificationHudHost host) {
        if (!claim(host)) return TextComponent.EMPTY;
        int count = NotificationManager.get().unreadCount();
        String name = BaniraComponent.get().transClientAuto("notification_minimap_label").toString();
        return new TextComponent(count == 0 ? name : name + " " + NotificationHudState.countLabel(count));
    }

    public static void reset() {
        HOSTS.reset();
    }

    private static long now() {
        return System.nanoTime() / 1_000_000;
    }
}
