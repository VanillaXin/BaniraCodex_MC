package xin.vanilla.banira.api;

import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 子 mod 推荐使用的配置注册与访问入口。
 */
public final class BaniraConfigs {

    private BaniraConfigs() {
    }

    public static <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        Banira.platform().configService().register(configClass, modId);
    }

    @Nonnull
    public static <T> T view(@Nonnull Class<?> configClass, @Nonnull Class<T> viewClass) {
        return Banira.platform().configService().view(configClass, viewClass);
    }

    /**
     * 返回加载器无关句柄；GUI 或内部编辑器需要的详细元数据仍由 common.config.ConfigHolder 承载。
     */
    @Nullable
    public static BaniraConfigHandle handle(@Nonnull Class<?> configClass) {
        return Banira.platform().configService().handle(configClass);
    }
}
