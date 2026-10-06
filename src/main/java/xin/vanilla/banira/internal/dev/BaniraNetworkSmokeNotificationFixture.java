package xin.vanilla.banira.internal.dev;

/**
 * Fixed transport input; expected order is independent of the batch planner.
 */
public final class BaniraNetworkSmokeNotificationFixture {
    public static final String PREFIX = "banira-smoke-native:";
    public static final int ENTRIES = 256;
    public static final int PAGES = 5;

    private BaniraNetworkSmokeNotificationFixture() {
    }

    public static String text(int index) {
        return index + "-abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ-0123456789";
    }
}
