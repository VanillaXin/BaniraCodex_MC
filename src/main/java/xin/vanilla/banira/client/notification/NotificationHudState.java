package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.common.enums.EnumNotificationHudMode;

public final class NotificationHudState {
    private boolean previousDown;
    private boolean toggled;
    private boolean shown;
    private EnumNotificationHudMode previousMode;

    public void update(EnumNotificationHudMode mode, boolean down, boolean active) {
        update(mode, down, active, active);
    }

    public void update(EnumNotificationHudMode mode, boolean down, boolean active, boolean acceptInput) {
        if (mode != previousMode) {
            toggled = false;
            previousDown = false;
        }
        previousMode = mode;
        if (active && acceptInput && down && !previousDown && mode == EnumNotificationHudMode.TOGGLE)
            toggled = !toggled;
        shown = active && (mode == EnumNotificationHudMode.ALWAYS
                || mode == EnumNotificationHudMode.HOLD && down
                || mode == EnumNotificationHudMode.TOGGLE && toggled);
        previousDown = down;
    }

    public void reset() {
        toggled = shown = previousDown = false;
        previousMode = null;
    }

    public boolean visible(int unread) {
        return shown;
    }

    public static String countLabel(int unread) {
        return unread <= 0 ? "" : unread > 99 ? "99+" : Integer.toString(unread);
    }

    public static int position(double relative, int screenSize, int size) {
        return (int) Math.round(Math.max(0, Math.min(1, relative)) * Math.max(0, screenSize - size));
    }

    public static double relative(double pixel, int screenSize, int size) {
        return Math.max(0, Math.min(1, pixel / Math.max(1, screenSize - size)));
    }
}
