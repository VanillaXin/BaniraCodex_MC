package xin.vanilla.banira.platform;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 加载器相关的 mod logo 注册服务；不支持动态元数据的平台可以提供 no-op 实现。
 */
public interface BaniraLogoService {

    void register(@Nonnull String modId, @Nonnull Supplier<String> logoFileSupplier);

    void register(@Nonnull Function<String, String> logoFileFunction);
}
