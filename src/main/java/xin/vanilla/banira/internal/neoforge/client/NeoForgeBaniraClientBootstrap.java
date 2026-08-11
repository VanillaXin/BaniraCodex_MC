package xin.vanilla.banira.internal.neoforge.client;

/**
 * NeoForge 入口与实际 GUI 启动类之间的轻量客户端桥。
 */
public final class NeoForgeBaniraClientBootstrap {
    private NeoForgeBaniraClientBootstrap() {
    }

    public static void init() {
        xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap.init();
        xin.vanilla.banira.internal.neoforge.compat.NeoForgeExternalInventoryCompatibility.init();
    }
}
