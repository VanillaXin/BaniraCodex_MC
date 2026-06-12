package xin.vanilla.banira.internal.forge.event;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraCommonSetupEvent;
import xin.vanilla.banira.api.event.BaniraLifecycle;

/**
 * Forge Mod 总线适配器，把加载器事件转换为 Banira 公共生命周期事件。
 */
public final class ForgeBaniraLifecycleAdapter {
    private ForgeBaniraLifecycleAdapter() {
    }

    public static void dispatchCommonSetup(FMLCommonSetupEvent event) {
        BaniraLifecycle.dispatchCommonSetup(BaniraCommonSetupEvent.withWorkQueue(event::enqueueWork));
    }
}
