package xin.vanilla.banira.client.notification;

import org.junit.Test;
import xin.vanilla.banira.client.data.NotificationLogEntry;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class NotificationHistoryTest {
    @Test
    public void arrivingDuplicatesAreSeparateUnreadEntries() {
        NotificationHistory history = new NotificationHistory();
        history.append(entry(1), 10);
        history.markRead(1);
        history.append(entry(2), 10);
        assertEquals(1, history.unreadCount());
        assertFalse(history.snapshot().get(0).read());
        assertTrue(history.snapshot().get(1).read());
    }

    @Test
    public void readingIsIdempotentAndMissingIdsDoNotChangeRevision() {
        NotificationHistory history = new NotificationHistory();
        history.append(entry(1), 10);
        assertTrue(history.markRead(1));
        long revision = history.revision();
        assertFalse(history.markRead(1));
        assertFalse(history.markRead(42));
        assertEquals(revision, history.revision());
        assertEquals(0, history.unreadCount());
    }

    @Test
    public void trimRemoveAndClearMaintainUnreadCount() {
        NotificationHistory history = new NotificationHistory();
        history.append(entry(1), 2);
        history.append(entry(2), 2);
        history.markRead(2);
        history.append(entry(3), 2);
        assertEquals(1, history.unreadCount());
        assertEquals(2, history.snapshot().size());
        assertTrue(history.remove(2));
        assertEquals(1, history.unreadCount());
        assertTrue(history.remove(3));
        assertEquals(0, history.unreadCount());
        history.append(entry(4), 2);
        assertTrue(history.clear());
        assertFalse(history.clear());
        assertEquals(0, history.unreadCount());
    }

    @Test
    public void loadAndMarkAllReadKeepIndependentSnapshots() {
        NotificationHistory history = new NotificationHistory();
        NotificationLogEntry original = entry(1);
        history.replace(Arrays.asList(original, entry(2).read(true)), 10);
        original.read(true);
        assertEquals(1, history.unreadCount());
        List<NotificationLogEntry> snapshot = history.snapshot();
        snapshot.get(0).read(true);
        assertFalse(history.snapshot().get(0).read());
        assertTrue(history.markAllRead());
        assertFalse(history.markAllRead());
        assertEquals(0, history.unreadCount());
    }

    @Test
    public void replacementTrimsAndRecounts() {
        NotificationHistory history = new NotificationHistory();
        history.replace(Arrays.asList(entry(3), entry(2).read(true), entry(1)), 2);
        assertEquals(1, history.unreadCount());
        assertEquals(3, history.snapshot().get(0).id());
    }

    private static NotificationLogEntry entry(long id) {
        return new NotificationLogEntry().id(id).componentJson("{\"text\":\"same\"}");
    }
}
