package xin.vanilla.banira.internal.neoforge.client;

import net.neoforged.bus.api.IEventBus;

/**
 * NeoForge 入口与实际 GUI 启动类之间的轻量客户端桥。
 */
public final class NeoForgeBaniraClientBootstrap {
    private NeoForgeBaniraClientBootstrap() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(BaniraClientModSetup::onClientSetup);
        modBus.addListener(BaniraClientModSetup::onRegisterKeyMappings);
        modBus.addListener(BaniraClientModSetup::onRegisterReloadListeners);
        modBus.addListener(NeoForgeNotificationLayerRegistrar::onAddGuiLayers);
        xin.vanilla.banira.internal.client.BaniraCodexClientBootstrap.init();
        xin.vanilla.banira.internal.neoforge.compat.NeoForgeExternalInventoryCompatibility.init();
    }
}
