package xin.vanilla.banira.platform;

import xin.vanilla.banira.common.config.ConfigHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 加载器无关的配置注册与访问服务。
 */
public interface BaniraConfigService {

    <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId);

    @Nonnull
    <T> T get(@Nonnull Class<T> configClass);

    @Nullable
    ConfigHolder holder(@Nonnull Class<?> configClass);
}
