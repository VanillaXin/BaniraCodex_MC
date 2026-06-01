package xin.vanilla.banira.internal.forge.platform;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.internal.forge.config.ForgeConfigAdapter;
import xin.vanilla.banira.platform.config.BaniraConfigService;

import javax.annotation.Nullable;

final class ForgeBaniraConfigService implements BaniraConfigService {
    @Override
    public <T> void register(Class<T> configClass, String modId) {
        ForgeConfigAdapter.register(configClass, modId);
    }

    @Override
    @Nullable
    public ConfigHolder getHolder(Class<?> configClass) {
        return ForgeConfigAdapter.getHolder(configClass);
    }

    @Override
    public <T> T get(Class<T> configClass) {
        return ForgeConfigAdapter.get(configClass);
    }
}
