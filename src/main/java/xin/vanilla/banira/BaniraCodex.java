package xin.vanilla.banira;

import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.internal.common.BaniraCodexRuntime;
import xin.vanilla.banira.internal.forge.ForgeBaniraCodexEntry;

@Mod(Banira.MOD_ID)
public class BaniraCodex {

    public BaniraCodex() {
        ForgeBaniraCodexEntry.bootstrap();
        BaniraCodexRuntime.bootstrap();
    }

}
