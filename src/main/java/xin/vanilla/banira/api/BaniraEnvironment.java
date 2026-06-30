package xin.vanilla.banira.api;

import javax.annotation.Nonnull;

/**
 * 子 mod 推荐使用的运行环境入口；加载器和版本差异由当前分支的 platform 实现负责。
 */
public final class BaniraEnvironment {

    private BaniraEnvironment() {
    }

    @Nonnull
    public static String loaderType() {
        return Banira.platform().loaderType();
    }

    @Nonnull
    public static String minecraftVersion() {
        return Banira.platform().minecraftVersion();
    }

    public static boolean isClient() {
        return Banira.platform().isClient();
    }

    public static boolean isDedicatedServer() {
        return Banira.platform().isDedicatedServer();
    }

    public static boolean isDevelopment() {
        return Banira.platform().isDevelopment();
    }

    public static boolean isProduction() {
        return !isDevelopment();
    }

    public static boolean isModLoaded(@Nonnull String modId) {
        return Banira.platform().isModLoaded(modId);
    }
}
