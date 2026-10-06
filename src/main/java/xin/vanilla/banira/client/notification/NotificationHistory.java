package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.client.data.NotificationLogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Owns read state independently of overlay visibility and duplicate coalescing.
 */
public final class NotificationHistory {
    private final List<NotificationLogEntry> entries = new ArrayList<>();
    private int unreadCount;
    private long revision;

    public synchronized void append(NotificationLogEntry entry, int maxEntries) {
        NotificationLogEntry copy = entry.copy();
        entries.add(0, copy);
        if (!copy.read()) unreadCount++;
        trim(maxEntries);
        revision++;
    }

    public synchronized void replace(List<NotificationLogEntry> loaded, int maxEntries) {
        entries.clear();
        unreadCount = 0;
        for (NotificationLogEntry entry : loaded) {
            entries.add(entry.copy());
            if (!entry.read()) unreadCount++;
        }
        trim(maxEntries);
        revision++;
    }

    private void trim(int maxEntries) {
        while (entries.size() > Math.max(1, maxEntries)) {
            if (!entries.remove(entries.size() - 1).read()) unreadCount--;
        }
    }

    public synchronized boolean markRead(long id) {
        for (NotificationLogEntry entry : entries) {
            if (entry.id() == id && !entry.read()) {
                entry.read(true);
                unreadCount--;
                revision++;
                return true;
            }
        }
        return false;
    }

    public synchronized boolean markAllRead() {
        if (unreadCount == 0) return false;
        for (NotificationLogEntry entry : entries) entry.read(true);
        unreadCount = 0;
        revision++;
        return true;
    }

    public synchronized boolean remove(long id) {
        Iterator<NotificationLogEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            NotificationLogEntry entry = iterator.next();
            if (entry.id() == id) {
                iterator.remove();
                if (!entry.read()) unreadCount--;
                revision++;
                return true;
            }
        }
        return false;
    }

    public synchronized boolean clear() {
        if (entries.isEmpty()) return false;
        entries.clear();
        unreadCount = 0;
        revision++;
        return true;
    }

    public synchronized List<NotificationLogEntry> snapshot() {
        List<NotificationLogEntry> snapshot = new ArrayList<>(entries.size());
        for (NotificationLogEntry entry : entries) snapshot.add(entry.copy());
        return Collections.unmodifiableList(snapshot);
    }

    public synchronized int unreadCount() {
        return unreadCount;
    }

    public synchronized long revision() {
        return revision;
    }
}
