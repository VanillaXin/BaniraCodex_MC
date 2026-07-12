package xin.vanilla.banira.api.client;

import xin.vanilla.banira.api.Banira;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 子 mod 注册动态 logo 的稳定客户端入口；元数据写入由当前加载器适配层完成。
 */
public final class BaniraLogos {
    private BaniraLogos() {
    }

    public static void register(@Nonnull String modId, @Nonnull Supplier<String> logoFileSupplier) {
        Banira.platform().logoService().register(modId, logoFileSupplier);
    }

    public static void register(@Nonnull Function<String, String> logoFileFunction) {
        Banira.platform().logoService().register(logoFileFunction);
    }
}
