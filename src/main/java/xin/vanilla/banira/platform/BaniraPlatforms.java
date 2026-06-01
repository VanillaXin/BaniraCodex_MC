package xin.vanilla.banira.platform;

import java.util.Objects;

/**
 * Runtime holder for the active loader implementation.
 */
public final class BaniraPlatforms {
    private static volatile BaniraPlatform platform;

    private BaniraPlatforms() {
    }

    public static void install(BaniraPlatform value) {
        platform = Objects.requireNonNull(value, "platform");
    }

    public static BaniraPlatform get() {
        BaniraPlatform current = platform;
        if (current == null) {
            throw new IllegalStateException("Banira platform has not been installed yet");
        }
        return current;
    }

    public static boolean isInstalled() {
        return platform != null;
    }
}
