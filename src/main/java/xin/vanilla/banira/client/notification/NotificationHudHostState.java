package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.common.enums.EnumNotificationHudHost;

import java.util.Arrays;

/**
 * Tracks live information-slot callbacks without retaining map objects.
 */
public final class NotificationHudHostState {
    private static final EnumNotificationHudHost[] HOSTS = EnumNotificationHudHost.values();
    private final long[] lastSeen = new long[HOSTS.length];
    private final boolean[] present = new boolean[HOSTS.length];

    public void seen(EnumNotificationHudHost host, long now) {
        if (host == null || host == EnumNotificationHudHost.AUTO || host == EnumNotificationHudHost.STANDALONE) return;
        lastSeen[host.ordinal()] = now;
        present[host.ordinal()] = true;
    }

    public EnumNotificationHudHost select(EnumNotificationHudHost preference, long now) {
        if (preference == EnumNotificationHudHost.AUTO) {
            for (EnumNotificationHudHost host : HOSTS) {
                if (active(host, now)) return host;
            }
        } else if (preference != null && active(preference, now)) {
            return preference;
        }
        return EnumNotificationHudHost.STANDALONE;
    }

    public EnumNotificationHudHost select(EnumNotificationHudHost preference, long now, boolean standaloneScene) {
        return standaloneScene ? EnumNotificationHudHost.STANDALONE : select(preference, now);
    }

    private boolean active(EnumNotificationHudHost host, long now) {
        return present[host.ordinal()] && now >= lastSeen[host.ordinal()] && now - lastSeen[host.ordinal()] <= 500;
    }

    public void reset() {
        Arrays.fill(present, false);
    }
}
