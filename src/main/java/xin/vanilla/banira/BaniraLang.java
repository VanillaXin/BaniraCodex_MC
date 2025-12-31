package xin.vanilla.banira;

import xin.vanilla.banira.common.util.Translator;


public final class BaniraLang extends Translator {

    public static final BaniraLang INSTANCE = new BaniraLang();

    private BaniraLang() {
        super(BaniraCodex.MODID);
    }
}
