package xin.vanilla.banira.common.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 配置系统的稳定入口；具体加载器的注册细节由内部适配器完成。
 */
public final class BaniraConfig {
    private BaniraConfig() {
    }

    public static <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        ForgeConfigAdapter.register(configClass, modId);
    }

    @Nonnull
    public static <T> T get(@Nonnull Class<T> configClass) {
        return ForgeConfigAdapter.get(configClass);
    }

    @Nullable
    public static ConfigHolder holder(@Nonnull Class<?> configClass) {
        return ForgeConfigAdapter.getHolder(configClass);
    }
}
