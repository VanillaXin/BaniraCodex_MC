package xin.vanilla.banira.internal.forge.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.internal.client.BaniraClientModSetup;
import xin.vanilla.banira.internal.client.BaniraKeyBindingService;

@Mod.EventBusSubscriber(modid = BaniraCodex.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForgeBaniraClientModSetup {
    private ForgeBaniraClientModSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BaniraKeyBindingService.installRegistrar(ClientRegistry::registerKeyBinding);
        BaniraClientModSetup.initOnClientSetup();
    }
}
