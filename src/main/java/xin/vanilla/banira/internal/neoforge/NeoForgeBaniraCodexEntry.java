package xin.vanilla.banira.internal.neoforge;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.neoforge.event.NeoForgeBaniraCommandAdapter;
import xin.vanilla.banira.internal.neoforge.event.NeoForgeBaniraGameEventAdapter;
import xin.vanilla.banira.internal.neoforge.event.NeoForgeBaniraLifecycleAdapter;
import xin.vanilla.banira.internal.neoforge.client.NeoForgeBaniraClientBootstrap;
import xin.vanilla.banira.internal.neoforge.config.NeoForgeConfigAdapter;
import xin.vanilla.banira.internal.neoforge.platform.NeoForgeBaniraPlatform;
import xin.vanilla.banira.internal.neoforge.network.NeoForgeNetworkChannels;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * NeoForge 入口胶水层：把加载器事件转换和服务安装限制在 internal 包内。
 */
public final class NeoForgeBaniraCodexEntry {

    private NeoForgeBaniraCodexEntry() {
    }

    public static void bootstrap(IEventBus modBus, ModContainer container) {
        BaniraPlatforms.installIfAbsent(new NeoForgeBaniraPlatform());
        NeoForgeConfigAdapter.install(modBus, container);

        // 配置必须在 CONFIG 加载阶段之前注册。
        BaniraConfigs.register(CommonConfig.class, Banira.MOD_ID);
        BaniraConfigs.register(ClientConfig.class, Banira.MOD_ID);

        modBus.addListener(NeoForgeBaniraLifecycleAdapter::dispatchCommonSetup);
        modBus.addListener(NeoForgeNetworkChannels::registerPayloadHandlers);

        NeoForge.EVENT_BUS.register(NeoForgeBaniraCommandAdapter.class);
        NeoForge.EVENT_BUS.register(NeoForgeBaniraGameEventAdapter.class);
        NetworkInit.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrapAccess.init();
        }
    }

    private static final class ClientBootstrapAccess {
        private static void init() {
            NeoForgeBaniraClientBootstrap.init();
        }
    }
}
