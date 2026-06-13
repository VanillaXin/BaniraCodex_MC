package xin.vanilla.banira.internal.config;

import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * 内部配置视图和公共 configService 之间的窄适配层。
 */
final class BaniraConfigHandles {
    private BaniraConfigHandles() {
    }

    static ConfigHolder holder(Class<?> configClass) {
        BaniraConfigHandle handle = BaniraPlatforms.get().configService().handle(configClass);
        if (handle instanceof BaniraConfigHandleAdapter) {
            return ((BaniraConfigHandleAdapter) handle).holder();
        }
        throw new IllegalStateException("Config holder is not available for " + configClass.getName());
    }
}
