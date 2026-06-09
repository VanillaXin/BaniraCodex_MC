package xin.vanilla.banira;

import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.internal.common.BaniraCodexRuntime;
import xin.vanilla.banira.internal.forge.ForgeBaniraCodexEntry;

@Mod(BaniraCodex.MODID)
public class BaniraCodex {

    public static final String MODID = "banira_codex";

    public BaniraCodex() {
        ForgeBaniraCodexEntry.bootstrap();
        BaniraCodexRuntime.bootstrap();
    }

}
