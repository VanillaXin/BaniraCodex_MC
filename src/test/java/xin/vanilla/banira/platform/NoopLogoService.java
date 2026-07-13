package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/** 测试默认使用的空 logo 服务。 */
public enum NoopLogoService implements BaniraLogoService {
    INSTANCE;

    @Override
    public void register(@Nonnull String modId, @Nonnull Supplier<String> logoFileSupplier) {
    }

    @Override
    public void register(@Nonnull Function<String, String> logoFileFunction) {
    }
}
