package xin.vanilla.banira;

import xin.vanilla.banira.api.Banira;
import xin.vanilla.banira.common.util.IIdentifier;

public final class Identifier implements IIdentifier {
    private static final IIdentifier instance = new Identifier();

    @Override
    public String modId() {
        return Banira.MOD_ID;
    }

    public IIdentifier instance() {
        return id();
    }

    public static IIdentifier id() {
        return instance;
    }
}
