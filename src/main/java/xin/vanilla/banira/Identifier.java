package xin.vanilla.banira;

import xin.vanilla.banira.common.util.IIdentifier;

public final class Identifier implements IIdentifier {
    private static final IIdentifier instance = new Identifier();

    @Override
    public String modId() {
        return BaniraCodex.MODID;
    }

    public IIdentifier instance() {
        return id();
    }

    public static IIdentifier id() {
        return instance;
    }
}
