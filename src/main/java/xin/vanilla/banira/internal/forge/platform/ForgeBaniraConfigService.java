package xin.vanilla.banira.internal.forge.platform;

import xin.vanilla.banira.internal.forge.config.ForgeConfigAdapter;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraConfigService;

import javax.annotation.Nullable;

final class ForgeBaniraConfigService implements BaniraConfigService {
    @Override
    public <T> void register(Class<T> configClass, String modId) {
        ForgeConfigAdapter.register(configClass, modId);
    }

    @Override
    public <T> T view(Class<?> configClass, Class<T> viewClass) {
        return ForgeConfigAdapter.view(configClass, viewClass);
    }

    @Override
    @Nullable
    public BaniraConfigHandle handle(Class<?> configClass) {
        return ForgeConfigAdapter.getHolder(configClass);
    }
}
