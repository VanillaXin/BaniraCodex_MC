package xin.vanilla.banira.internal.fabric.client;

import xin.vanilla.banira.client.util.LogoModifier;
import xin.vanilla.banira.platform.BaniraLogoService;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fabric 动态 logo 注册适配；实际图标仍由 fabric.mod.json 决定。
 */
public enum FabricLogoService implements BaniraLogoService {
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
