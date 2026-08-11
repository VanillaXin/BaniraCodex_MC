package xin.vanilla.banira;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.internal.common.BaniraCodexRuntime;
import xin.vanilla.banira.internal.neoforge.NeoForgeBaniraCodexEntry;

/** NeoForge 模组入口，具体加载器接线集中在 internal 适配层。 */
@Mod(Banira.MOD_ID)
public final class BaniraCodex {

    public BaniraCodex(IEventBus modBus, ModContainer container) {
        NeoForgeBaniraCodexEntry.bootstrap(modBus, container);
        BaniraCodexRuntime.bootstrap();
    }
}
