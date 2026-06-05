package xin.vanilla.banira.common.config;

import xin.vanilla.banira.api.Banira;

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

    @Nullable
    public static ConfigHolder holder(@Nonnull Class<?> configClass) {
        return Banira.platform().configService().holder(configClass);
    }
}
