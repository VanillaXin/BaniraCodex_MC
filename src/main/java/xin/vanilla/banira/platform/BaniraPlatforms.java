package xin.vanilla.banira.platform;

import java.util.Objects;

/**
 * 当前加载器 platform 的运行时持有者。
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
