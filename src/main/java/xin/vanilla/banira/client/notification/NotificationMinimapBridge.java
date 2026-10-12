package xin.vanilla.banira.client.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TranslatableComponent;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.hud.MinimapLine;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumNotificationHudHost;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.client.minimap.MinimapLineRegistry;
import xin.vanilla.banira.internal.client.minimap.MinimapLineSnapshot;

import java.util.Collections;
import java.util.List;

/**
 * Optional map callbacks share the existing HUD visibility state.
 */
public final class NotificationMinimapBridge {
    private static boolean registered;

    private NotificationMinimapBridge() {
    }

    public static EnumNotificationHudHost selectedHost() {
        if (Minecraft.getInstance().screen instanceof ChatScreen) return EnumNotificationHudHost.STANDALONE;
        return MinimapLineRegistry.shared().selectedHost(ClientConfig.get().notificationHud().host(), now());
    }

    public static boolean claim(EnumNotificationHudHost host) {
        if (!NotificationUnreadHud.isMinimapVisible()) return false;
        long now = now();
        MinimapLineRegistry.shared().seen(host, now);
        return MinimapLineRegistry.shared().selectedHost(ClientConfig.get().notificationHud().host(), now) == host;
    }

    public static Component text(EnumNotificationHudHost host) {
        if (!claim(host)) return TextComponent.EMPTY;
        return notificationText();
    }

    private static Component notificationText() {
        if (!NotificationUnreadHud.isMinimapVisible()) return TextComponent.EMPTY;
        int count = NotificationManager.get().unreadCount();
        String name = count == 0 ? BaniraComponent.get().transClientAuto("notification_minimap_empty").toString()
                : BaniraComponent.get().transClientAuto("notification_minimap_unread", count).toString();
        return new TextComponent(name);
    }

    public static void reset() {
        MinimapLineRegistry.shared().resetHosts();
    }

    public static List<MinimapLine> registrations() {
        ensureRegistered();
        return MinimapLineRegistry.shared().registrations();
    }

    public static List<MinimapLineSnapshot> rows(EnumNotificationHudHost host) {
        ensureRegistered();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return Collections.emptyList();
        long now = now();
        MinimapLineRegistry.shared().seen(host, now);
        return MinimapLineRegistry.shared().snapshot(host, now);
    }

    private static synchronized void ensureRegistered() {
        if (registered) return;
        registered = true;
        MinimapLineRegistry.shared().register(new MinimapLine(new ResourceLocation("banira_codex", "unread"),
                new TranslatableComponent("word.banira_codex.notification_unread"),
                NotificationMinimapBridge::notificationText, () -> ClientConfig.get().notificationHud().host(), 100));
    }

    private static long now() {
        return System.nanoTime();
    }
}
