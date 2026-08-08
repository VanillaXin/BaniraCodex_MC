package xin.vanilla.banira.internal.forge.client;

/**
 * Forge 入口与实际 GUI 启动类之间的轻量客户端桥。
 */
public final class ForgeBaniraClientBootstrap {
    private ForgeBaniraClientBootstrap() {
    }

    public static void init() {
        xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap.init();
        xin.vanilla.banira.internal.forge.compat.ForgeExternalInventoryCompatibility.init();
    }
}
