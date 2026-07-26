package xin.vanilla.banira.internal.neoforge.client;

import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.platform.BaniraLogoService;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * NeoForge mod 元数据 logo 注册适配。
 */
public enum NeoForgeLogoService implements BaniraLogoService {
    INSTANCE;

    @Override
    public void register(@Nonnull String modId, @Nonnull Supplier<String> logoFileSupplier) {
        LogoModifier.register(modId, logoFileSupplier);
    }

    @Override
    public void register(@Nonnull Function<String, String> logoFileFunction) {
        LogoModifier.register(logoFileFunction);
    }
}
