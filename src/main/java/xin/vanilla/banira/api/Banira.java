package xin.vanilla.banira.api;

import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * 子 mod 的稳定入口；加载器差异统一藏在 platform 实现里。
 */
public final class Banira {
    private Banira() {
    }

    public static BaniraPlatform platform() {
        return BaniraPlatforms.get();
    }
}
