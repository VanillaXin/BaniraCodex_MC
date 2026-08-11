package xin.vanilla.banira.internal.neoforge.event;

import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraLifecycle;

/**
 * NeoForge MOD 总线适配器，把加载器事件转换为 Banira 公共生命周期事件。
 */
public final class NeoForgeBaniraLifecycleAdapter {
    private NeoForgeBaniraLifecycleAdapter() {
    }

    public static void dispatchCommonSetup(FMLCommonSetupEvent event) {
        BaniraLifecycle.dispatchCommonSetup(BaniraCommonSetupEvent.withWorkQueue(event::enqueueWork));
    }
}
