package xin.vanilla.banira.internal.forge.config;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraConfigService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Forge 分支的配置服务实现，负责桥接到 ForgeConfigSpec 适配器。
 */
public enum ForgeBaniraConfigService implements BaniraConfigService {
    INSTANCE;

    @Override
    public <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        ForgeConfigAdapter.register(configClass, modId);
    }

    @Nonnull
    @Override
    public <T> T get(@Nonnull Class<T> configClass) {
        return ForgeConfigAdapter.get(configClass);
    }

    @Nullable
    @Override
    public ConfigHolder holder(@Nonnull Class<?> configClass) {
        return ForgeConfigAdapter.getHolder(configClass);
    }
}
