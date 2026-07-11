package xin.vanilla.banira.api;

import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.banira.internal.forge.platform.ForgeBaniraPlatform;

/**
 * 子 mod 的稳定入口；加载器差异统一藏在 platform 实现里。
 */
public final class Banira {
    /**
     * Banira Codex 自身的稳定 mod id；子 mod 不应再依赖加载器入口类取这个值。
     */
    public static final String MOD_ID = "banira_codex";

    private Banira() {
    }

    public static BaniraPlatform platform() {
        // Forge 20.1 可能先构造子 mod；公共 API 入口需要能在构造阶段懒安装平台。
        if (!BaniraPlatforms.isInstalled()) {
            BaniraPlatforms.install(new ForgeBaniraPlatform());
        }
        return BaniraPlatforms.get();
    }
}
