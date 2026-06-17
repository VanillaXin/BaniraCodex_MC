package xin.vanilla.banira.common.config;

import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 配置系统的稳定入口；具体加载器的注册细节由内部适配器完成。
 */
public final class BaniraConfig {
    private BaniraConfig() {
    }

    public static <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        Banira.platform().configService().register(configClass, modId);
    }

    @Nonnull
    public static <T> T get(@Nonnull Class<T> configClass) {
        return Banira.platform().configService().get(configClass);
    }

    @Nonnull
    public static <T> T view(@Nonnull Class<?> configClass, @Nonnull Class<T> viewClass) {
        return Banira.platform().configService().view(configClass, viewClass);
    }

    @Nullable
    public static ConfigHolder holder(@Nonnull Class<?> configClass) {
        BaniraConfigHandle handle = handle(configClass);
        if (handle == null) {
            return null;
        }
        if (handle instanceof ConfigHolder) {
            return (ConfigHolder) handle;
        }
        throw new IllegalStateException("Config handle is not a ConfigHolder: " + handle.getClass().getName());
    }

    @Nullable
    public static BaniraConfigHandle handle(@Nonnull Class<?> configClass) {
        return Banira.platform().configService().handle(configClass);
    }
}
