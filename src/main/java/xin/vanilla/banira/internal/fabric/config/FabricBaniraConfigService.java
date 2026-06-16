package xin.vanilla.banira.internal.fabric.config;

import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Fabric 分支的配置服务实现。
 */
public enum FabricBaniraConfigService implements BaniraConfigService {
    INSTANCE;

    @Override
    public <T> void register(@Nonnull Class<T> configClass, @Nonnull String modId) {
        FabricConfigAdapter.register(configClass, modId);
    }

    @Nonnull
    @Override
    public <T> T get(@Nonnull Class<T> configClass) {
        return FabricConfigAdapter.get(configClass);
    }

    @Nullable
    @Override
    public BaniraConfigHandle handle(@Nonnull Class<?> configClass) {
        return FabricConfigAdapter.getHolder(configClass);
    }
}
