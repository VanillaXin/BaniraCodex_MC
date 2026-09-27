package xin.vanilla.banira.client.notification;

import xin.vanilla.banira.common.enums.EnumNotificationHudMode;

public final class NotificationHudState {
    private boolean previousDown;
    private boolean toggled;
    private boolean shown;
    private EnumNotificationHudMode previousMode;

    public void update(EnumNotificationHudMode mode, boolean down, boolean active) {
        if (!active || mode != previousMode) {
            toggled = false;
            previousDown = false;
        }
        previousMode = mode;
        if (active && down && !previousDown && mode == EnumNotificationHudMode.TOGGLE) toggled = !toggled;
        shown = active && (mode == EnumNotificationHudMode.ALWAYS
                || mode == EnumNotificationHudMode.HOLD && down
                || mode == EnumNotificationHudMode.TOGGLE && toggled);
        previousDown = down;
    }

    public boolean visible(int unread) { return shown && unread > 0; }

    public static String countLabel(int unread) { return unread > 99 ? "99+" : Integer.toString(Math.max(0, unread)); }

    public static int position(double relative, int screenSize, int size) {
        return (int) Math.round(Math.max(0, Math.min(1, relative)) * Math.max(0, screenSize - size));
    }

    public static double relative(double pixel, int screenSize, int size) {
        return Math.max(0, Math.min(1, pixel / Math.max(1, screenSize - size)));
    }
}
