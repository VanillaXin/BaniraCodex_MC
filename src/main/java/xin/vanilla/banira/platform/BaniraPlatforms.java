package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 当前加载器 platform 的运行时持有者。
 */
public final class BaniraPlatforms {
    private static volatile BaniraPlatform platform;

    private BaniraPlatforms() {
    }

    public static void install(@Nonnull BaniraPlatform value) {
        platform = Objects.requireNonNull(value, "platform");
    }

    @Nonnull
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
