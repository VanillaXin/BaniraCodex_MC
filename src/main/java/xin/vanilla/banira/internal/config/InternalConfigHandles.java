package xin.vanilla.banira.internal.config;

import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraConfigHandle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Banira 自身配置使用的内部句柄转换；子 mod 应通过 api.BaniraConfigs 访问配置。
 */
final class InternalConfigHandles {

    private InternalConfigHandles() {
    }

    @Nullable
    static ConfigHolder holder(@Nonnull Class<?> configClass) {
        BaniraConfigHandle handle = BaniraConfigs.handle(configClass);
        if (handle == null) {
            return null;
        }
        if (handle instanceof ConfigHolder) {
            return (ConfigHolder) handle;
        }
        throw new IllegalStateException("Config handle is not a ConfigHolder: " + handle.getClass().getName());
    }
}
