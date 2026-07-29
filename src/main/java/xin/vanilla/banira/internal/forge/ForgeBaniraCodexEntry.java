package xin.vanilla.banira.internal.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.BaniraScheduler;
import xin.vanilla.banira.internal.config.ClientConfig;
import xin.vanilla.banira.internal.config.CommonConfig;
import xin.vanilla.banira.internal.forge.client.ForgeBaniraClientBootstrap;
import xin.vanilla.banira.internal.forge.event.ForgeBaniraEventBridge;
import xin.vanilla.banira.internal.forge.platform.ForgeBaniraPlatform;
import xin.vanilla.banira.internal.network.NetworkInit;
import xin.vanilla.banira.platform.BaniraPlatforms;

@Mod(BaniraCodex.MODID)
public final class ForgeBaniraCodexEntry {
    public ForgeBaniraCodexEntry() {
        BaniraPlatforms.installIfAbsent(new ForgeBaniraPlatform());

        // Config specs must be registered before Forge finishes loading CONFIG specs.
        BaniraPlatforms.get().configService().register(CommonConfig.class, BaniraCodex.MODID);
        BaniraPlatforms.get().configService().register(ClientConfig.class, BaniraCodex.MODID);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((FMLCommonSetupEvent event) ->
                BaniraEventBus.dispatchCommonSetup(BaniraCommonSetupEvent.withWorkQueue(event::enqueueWork)));

        MinecraftForge.EVENT_BUS.register(ForgeBaniraEventBridge.class);
        BaniraScheduler.init();
        NetworkInit.register();
        BaniraCodex.bootstrapCommon();

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ForgeBaniraClientBootstrap::init);
    }
}
