package xin.vanilla.banira.internal.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.gui.NotificationLogScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.widget.ButtonWidget;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.internal.client.NotificationLogStore;
import xin.vanilla.banira.internal.dev.BaniraNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/** Runs only within the existing opt-in development smoke, using its isolated client data. */
final class NotificationReadSmoke {
    private static final String TYPE = "banira_codex:read_smoke";
    private static int ticks;
    private static boolean finished;
    private static Notification bubble;
    private static NotificationLogScreen screen;
    private static long selectedId;

    private NotificationReadSmoke() { }

    static boolean tick(Minecraft client) throws Exception {
        if (finished) return true;
        NotificationManager manager = NotificationManager.get();
        if ("phase-two".equals(BaniraNetworkSmokeStatus.phase())) {
            List<NotificationLogEntry> saved = manager.getLog();
            long read = saved.stream().filter(e -> TYPE.equals(e.notificationType()) && e.read()).count();
            long unread = saved.stream().filter(e -> TYPE.equals(e.notificationType()) && !e.read()).count();
            require(read == 4 && unread == 1, "read states did not survive restart: " + read + "/" + unread);
            require(!manager.overlaysMuted(), "mute persisted into new client session");
            manager.muteForFiveMinutes();
            // The existing remote vanilla batch must still arrive while overlay mute is active.
            BaniraNetworkSmokeStatus.append("PASS notification-read-restart read=4 unread=1 vanilla-muted=true");
            finished = true;
            return true;
        }

        switch (ticks++) {
            case 0:
                manager.markAllRead();
                NotificationTypeSettingsStore.get().getOrCreate(TYPE).displayMode(EnumNotificationTypeDisplayMode.OVERLAY);
                bubble = add("Notification history smoke", false);
                add("Notification history smoke", true);
                require(manager.unreadCount() == 2, "duplicate arrivals were not counted separately");
                require(bubble.coalesceCount() == 2, "duplicate overlay did not coalesce");
                selectedId = manager.getLog().get(0).id();
                screen = new NotificationLogScreen(null);
                client.setScreen(screen);
                require(manager.unreadCount() == 2, "opening history implicitly marked a row read");
                break;
            case 20:
                click(screen, ((Number) field(screen, "listX")).doubleValue() + 15,
                        ((Number) field(screen, "listY")).doubleValue() + 12);
                require(manager.unreadCount() == 1, "selecting a row did not mark exactly one record read");
                add("A new unread notification", false);
                break;
            case 40:
                require(selectedId == selectedEntry().id(), "incoming record changed the selected detail");
                require(manager.unreadCount() == 2, "live refresh changed unread state");
                screen.resize(client, screen.width, screen.height);
                require(selectedId == selectedEntry().id(), "resizing history changed the selected detail");
                require(manager.tryHandleHudClick(bubble.closeBtnLeft() + 2, bubble.closeBtnTop() + 2, 0),
                        "close control was not clickable");
                require(manager.overlaysMuted(), "close control did not enable mute");
                Notification muted = add("Received during mute", false);
                require(manager.unreadCount() == 3, "muted notification was not recorded as unread");
                Map<?, ?> overlays = (Map<?, ?>) field(manager, "notifications");
                for (Object group : overlays.values()) require(!((List<?>) group).contains(muted), "muted bubble was queued");
                break;
            case 60:
                screenshot(client);
                ButtonWidget allRead = (ButtonWidget) field(screen, "markAllReadButton");
                click(screen, allRead.bounds().x() + 12, allRead.bounds().y() + 10);
                require(manager.unreadCount() == 0, "mark-all button failed");
                add("Unread after mark-all", false);
                NotificationLogStore.flush();
                require(NotificationLogStore.load(500).stream()
                        .filter(e -> TYPE.equals(e.notificationType()) && !e.read()).count() == 1,
                        "flushed read states differ from memory");
                BaniraNetworkSmokeStatus.append("PASS notification-read-ui duplicates=2 selected-stable=true muted-arrival=true mark-all=true");
                client.setScreen(null);
                finished = true;
                return true;
            default:
                break;
        }
        return false;
    }

    private static Notification add(String text, boolean network) {
        Notification notification = Notification.ofComponent(BaniraComponent.get().literal(text));
        notification.notificationType(TYPE);
        notification.durationTime(60000);
        NotificationManager.get().addNotification(notification, network);
        return notification;
    }

    private static void click(NotificationLogScreen target, double x, double y) {
        target.mouseClicked(x, y, 0);
        target.mouseReleased(x, y, 0);
    }

    @SuppressWarnings("unchecked")
    private static NotificationLogEntry selectedEntry() throws ReflectiveOperationException {
        List<NotificationLogEntry> entries = (List<NotificationLogEntry>) field(screen, "filteredEntries");
        int index = (Integer) field(screen, "selectedIndex");
        require(index >= 0 && index < entries.size(), "selection disappeared");
        return entries.get(index);
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void screenshot(Minecraft client) throws Exception {
        Path status = Paths.get(System.getProperty("banira.networkSmoke.status"));
        Path output = status.getParent().resolve("notification-history-read.png");
        Files.createDirectories(output.getParent());
        try (NativeImage image = Screenshot.takeScreenshot(client.getWindow().getWidth(),
                client.getWindow().getHeight(), client.getMainRenderTarget())) {
            image.writeToFile(output);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
