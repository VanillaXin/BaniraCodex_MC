package xin.vanilla.banira.internal.forge.platform;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.internal.config.BaniraConfigHandleAdapter;
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
    public <T> T get(Class<T> configClass) {
        return ForgeConfigAdapter.get(configClass);
    }

    @Override
    @Nullable
    public BaniraConfigHandle handle(Class<?> configClass) {
        ConfigHolder holder = ForgeConfigAdapter.getHolder(configClass);
        return holder != null ? new BaniraConfigHandleAdapter(holder) : null;
    }
}
