package xin.vanilla.banira.platform.config;

import xin.vanilla.banira.common.config.ConfigHolder;

import javax.annotation.Nullable;

/**
 * Loader-neutral configuration registration surface.
 */
public interface BaniraConfigService {
    <T> void register(Class<T> configClass, String modId);

    @Nullable
    ConfigHolder getHolder(Class<?> configClass);

    <T> T get(Class<T> configClass);
}
