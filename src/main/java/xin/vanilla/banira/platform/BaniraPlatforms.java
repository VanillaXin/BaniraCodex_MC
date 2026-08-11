package xin.vanilla.banira.platform;

import java.util.Objects;
import java.util.ServiceLoader;

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

    /**
     * 供加载器入口使用；若依赖 mod 已触发惰性发现，则保留同一平台实例及其注册状态。
     */
    public static synchronized BaniraPlatform installIfAbsent(BaniraPlatform value) {
        if (platform == null) {
            platform = Objects.requireNonNull(value, "platform");
        }
        return platform;
    }

    public static BaniraPlatform get() {
        BaniraPlatform current = platform;
        if (current == null) {
            current = discover();
        }
        return current;
    }

    private static synchronized BaniraPlatform discover() {
        if (platform != null) {
            return platform;
        }
        for (BaniraPlatform candidate : ServiceLoader.load(BaniraPlatform.class,
                BaniraPlatforms.class.getClassLoader())) {
            platform = candidate;
            return candidate;
        }
        throw new IllegalStateException("Banira platform has not been installed and no provider was found");
    }

    public static boolean isInstalled() {
        return platform != null;
    }
}
