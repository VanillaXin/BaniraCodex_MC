package xin.vanilla.banira.common.config;

import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 配置系统旧入口；子 mod 推荐使用 {@link BaniraConfigs}。
 */
public final class BaniraConfig {
    private BaniraConfig() {
    }

    public static <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        BaniraConfigs.register(configClass, modId);
    }

    @Nonnull
    public static <T> T view(@Nonnull Class<?> configClass, @Nonnull Class<T> viewClass) {
        return BaniraConfigs.view(configClass, viewClass);
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
        return BaniraConfigs.handle(configClass);
    }
}
