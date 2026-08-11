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
            current = loadPlatform();
        }
        return current;
    }

    private static synchronized BaniraPlatform loadPlatform() {
        if (platform == null) {
            // 子 mod 可能先于 Banira 入口构造，provider 允许平台在此时按需安装。
            platform = ServiceLoader.load(BaniraPlatform.class)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No Banira platform provider was found"));
        }
        return platform;
    }

    public static boolean isInstalled() {
        return platform != null;
    }
}
