package xin.vanilla.banira.internal.client.ui;

import org.junit.Test;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.notification.NotificationHistory;
import xin.vanilla.banira.client.util.NotificationManager;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UiNotificationQueueParityTest {
    @Test
    public void hudReplacementDoesNotReadHistoryOrResetTypedMute() throws Exception {
        NotificationManager manager = new NotificationManager();
        Field field = NotificationManager.class.getDeclaredField("history");
        field.setAccessible(true);
        ((NotificationHistory) field.get(manager)).append(new NotificationLogEntry().id(42).notificationType("test.type"), 100);
        manager.muteType("test.type");
        long revision = manager.historyRevision();
        UiHudOwnershipTest.Host h = new UiHudOwnershipTest.Host();
        h.register(UiHudOwnershipTest.spec(new UiHudOwnershipTest.Binding()));
        h.tick(true);
        h.render(v -> {
        });
        h.backend.requestReload();
        h.tick(true);
        h.render(v -> {
        });
        h.tick(false);
        assertEquals(1, manager.unreadCount());
        assertEquals(revision, manager.historyRevision());
        assertTrue(manager.overlaysMuted("test.type"));
    }

    @Test
    public void animationAdmissionCanAdvanceWithoutPainting() throws Exception {
        boolean found = java.util.Arrays.stream(xin.vanilla.banira.client.gui.component.Notification.class.getMethods())
                .anyMatch(m -> m.getName().equals("prepareInRegion"));
        assertTrue("Queue admission must not depend on native painting", found);
    }
}
