package xin.vanilla.banira.internal.neoforge.config;

import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * NeoForge 分支的配置服务实现，负责桥接到 ModConfigSpec 适配器。
 */
public enum NeoForgeBaniraConfigService implements BaniraConfigService {
    INSTANCE;

    @Override
    public <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        NeoForgeConfigAdapter.register(configClass, modId);
    }

    @Nonnull
    @Override
    public <T> T view(@Nonnull Class<?> configClass, @Nonnull Class<T> viewClass) {
        return NeoForgeConfigAdapter.view(configClass, viewClass);
    }

    @Nullable
    @Override
    public BaniraConfigHandle handle(@Nonnull Class<?> configClass) {
        return NeoForgeConfigAdapter.getHolder(configClass);
    }
}
