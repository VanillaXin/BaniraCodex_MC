package xin.vanilla.banira.internal.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.forge.event.ForgeBaniraCommandAdapter;
import xin.vanilla.banira.internal.forge.event.ForgeBaniraGameEventAdapter;
import xin.vanilla.banira.internal.forge.event.ForgeBaniraLifecycleAdapter;
import xin.vanilla.banira.internal.forge.platform.ForgeBaniraPlatform;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraPlatforms;

/**
 * Forge 入口胶水层：把 FML/Forge 事件转换和服务安装限制在 internal 包内。
 */
public final class ForgeBaniraCodexEntry {

    private ForgeBaniraCodexEntry() {
    }

    public static void bootstrap() {
        BaniraPlatforms.installIfAbsent(new ForgeBaniraPlatform());

        // 配置必须在 CONFIG 加载阶段之前注册。
        BaniraConfigs.register(CommonConfig.class, Banira.MOD_ID);
        BaniraConfigs.register(ClientConfig.class, Banira.MOD_ID);

        @SuppressWarnings("removal")
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ForgeBaniraLifecycleAdapter::dispatchCommonSetup);

        MinecraftForge.EVENT_BUS.register(ForgeBaniraCommandAdapter.class);
        MinecraftForge.EVENT_BUS.register(ForgeBaniraGameEventAdapter.class);
        NetworkInit.register();
    }
}
