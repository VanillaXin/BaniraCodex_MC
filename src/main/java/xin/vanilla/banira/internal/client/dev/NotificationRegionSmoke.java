package xin.vanilla.banira.internal.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.client.gui.NotificationHudPositionScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.notification.NotificationHudState;
import xin.vanilla.banira.client.notification.NotificationUnreadHud;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumNotificationHudMode;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.ClientConfigView;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Isolated opt-in smoke for HUD editing and overlay admission; never loaded by normal gameplay.
 */
final class NotificationRegionSmoke {
    private static int tick;
    private static boolean finished;
    private static NotificationHudPositionScreen editor;
    private static final List<Notification> arrivals = new ArrayList<>();

    static boolean tick(Minecraft mc) throws Exception {
        if (finished) return true;
        ClientConfigView cfg = ClientConfig.get();
        NotificationManager manager = NotificationManager.get();
        if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) {
            require(Math.abs(cfg.notificationHud().x() - .25) < .01 && Math.abs(cfg.notificationHud().y() - .25) < .01,
                    "HUD coordinates were not persisted");
            require(cfg.notificationHud().mode() == EnumNotificationHudMode.TOGGLE
                    && cfg.notificationHud().keys().contains("Tab"), "HUD defaults changed across restart");
            BaniraNetworkSmokeStatus.append("PASS notification-region-restart position=true keys=Tab mode=TOGGLE");
            finished = true;
            return true;
        }
        switch (tick++) {
            case 0:
                for (List<Notification> group : overlays().values()) group.forEach(Notification::dismiss);
                cfg.notificationLogMaxEntries(500);
                cfg.notificationHud().mode(EnumNotificationHudMode.ALWAYS);
                if ("notification-region-ui".equals(BaniraNetworkSmokeStatus.phase())) {
                    ClientConfig.NotificationHudCategory defaults = new ClientConfig.NotificationHudCategory();
                    cfg.notificationHud().x(defaults.x()).y(defaults.y());
                    mc.setScreen(new NotificationHudPositionScreen(null));
                }
                cfg.notificationRegions().queueLimit(8).visibleLimit(2);
                cfg.notificationRegions().topLeft().visibleLimit(1);
                cfg.notificationRegions().bottomRight().visibleLimit(1);
                manager.markAllRead();
                for (int i = 0; i < 120; i++) {
                    Notification n = Notification.ofComponent(BaniraComponent.get().literal("Notification " + i
                            + "\nLong content remains available in notification history\n0123456789 abcdefghijklmnopqrstuvwxyz\nMore content\nLast content"));
                    n.notificationType("banira_codex:region_smoke");
                    n.position(i % 2 == 0 ? EnumPosition.TOP_LEFT : EnumPosition.BOTTOM_RIGHT);
                    n.durationTime(60000);
                    manager.addNotification(n);
                    arrivals.add(n);
                }
                require(manager.unreadCount() == 120, "queue overflow discarded unread history");
                require(overlays().values().stream().flatMap(List::stream).filter(n -> !n.finished()).count() == 8,
                        "overlay queue exceeded capacity");
                break;
            case 25:
                List<Notification> visible = drawOrder();
                require(visible.size() == 2, "global/per-region limit failed: " + visible.size());
                for (Notification n : visible) {
                    require(n.cachedWidth() <= mc.getWindow().getGuiScaledWidth() * .3 + 1, "bubble too wide");
                    require(n.cachedHeight() <= mc.getWindow().getGuiScaledHeight() * .32 + 1, "bubble too tall");
                    require(n.containsPoint(n.closeBtnLeft() + 2, n.closeBtnTop() + 2), "close control clipped");
                }
                require(arrivals.get(2).lastRenderTime() == 0, "queued bubble rendered before admission");
                screenshot(mc, "notification-regions-hud.png");
                for (Notification n : visible) if (n.position() == EnumPosition.TOP_LEFT) n.dismiss();
                cfg.notificationRegions().topLeft().visibleLimit(2);
                int stagger = cfg.notificationBurstStaggerMs();
                cfg.notificationBurstStaggerMs(0);
                Notification competing = Notification.ofComponent(BaniraComponent.get().literal("Competing region"));
                competing.notificationType("banira_codex:region_smoke");
                competing.position(EnumPosition.TOP_RIGHT);
                competing.durationTime(60000);
                manager.addNotification(competing);
                cfg.notificationBurstStaggerMs(stagger);
                break;
            case 45:
                require(arrivals.get(2).lastRenderTime() > 0, "queued bubble did not enter a freed region");
                require(drawOrder().contains(arrivals.get(1)), "new upper-region bubble preempted a visible lower-region bubble");
                for (List<Notification> group : overlays().values()) group.forEach(Notification::dismiss);
                editor = new NotificationHudPositionScreen(null);
                mc.setScreen(editor);
                break;
            case 55:
                int x = NotificationHudState.position(cfg.notificationHud().x(), editor.width, NotificationUnreadHud.WIDTH);
                int y = NotificationHudState.position(cfg.notificationHud().y(), editor.height, NotificationUnreadHud.HEIGHT);
                require(editor.mouseClicked(x + 4, y + 4, 0), "HUD preview cannot be grabbed");
                editor.mouseDragged((editor.width - NotificationUnreadHud.WIDTH) * .25 + 4,
                        (editor.height - NotificationUnreadHud.HEIGHT) * .25 + 4, 0, 5, 5);
                editor.mouseReleased(0, 0, 0);
                require(!editor.shouldCloseOnEsc(), "dirty HUD editor permits accidental close");
                editor.keyPressed(256, 0, 0);
                require(mc.screen == editor, "Escape discarded unsaved HUD position");
                editor.keyPressed(69, 0, 0);
                require(mc.screen == editor, "inventory key discarded unsaved HUD position");
                editor.mouseClicked(0, 0, 3);
                editor.mouseReleased(0, 0, 3);
                require(mc.screen == editor, "mouse back discarded unsaved HUD position");
                break;
            case 65:
                screenshot(mc, "notification-hud-position.png");
                ButtonWidget save = (ButtonWidget) editor.getWidget("save");
                editor.mouseClicked(save.bounds().x() + 5, save.bounds().y() + 5, 0);
                editor.mouseReleased(save.bounds().x() + 5, save.bounds().y() + 5, 0);
                require(Math.abs(cfg.notificationHud().x() - .25) < .01, "HUD save failed");
                cfg.notificationHud().mode(EnumNotificationHudMode.TOGGLE);
                cfg.notificationRegions().queueLimit(64).visibleLimit(6);
                cfg.notificationRegions().topLeft().visibleLimit(3);
                cfg.notificationRegions().bottomRight().visibleLimit(3);
                cfg.notificationLogMaxEntries(500);
                BaniraConfigs.save(ClientConfig.class);
                mc.setScreen(null);
                BaniraNetworkSmokeStatus.append("PASS notification-regions queue=8 history=120 visible=2 admission=true no-preemption=true hud-save=true close-shortcuts=true");
                finished = true;
                return true;
            default:
                break;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<EnumPosition, List<Notification>> overlays() throws Exception {
        return (Map<EnumPosition, List<Notification>>) field("notifications");
    }

    @SuppressWarnings("unchecked")
    private static List<Notification> drawOrder() throws Exception {
        return new ArrayList<>((List<Notification>) field("frameDrawOrder"));
    }

    private static Object field(String name) throws Exception {
        Field field = NotificationManager.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(NotificationManager.get());
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }

    private static void screenshot(Minecraft mc, String name) throws Exception {
        Path output = Paths.get(System.getProperty("banira.networkSmoke.status")).getParent().resolve(name);
        try (NativeImage image = Screenshot.takeScreenshot(mc.getWindow().getWidth(), mc.getWindow().getHeight(), mc.getMainRenderTarget())) {
            image.writeToFile(output);
        }
    }
}
