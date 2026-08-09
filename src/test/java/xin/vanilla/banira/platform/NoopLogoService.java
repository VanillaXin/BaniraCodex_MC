package xin.vanilla.banira.platform;

import java.util.function.Function;
import java.util.function.Supplier;

public enum NoopLogoService implements BaniraLogoService {
    INSTANCE;

    @Override
    public void register(String modId, Supplier<String> logoFileSupplier) {
    }

    @Override
    public void register(Function<String, String> logoFileFunction) {
    }
}
