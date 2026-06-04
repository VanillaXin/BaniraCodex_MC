package xin.vanilla.banira.client.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.banira.api.client.BaniraInput;
import xin.vanilla.banira.api.client.BaniraKeyHandle;
import xin.vanilla.banira.api.client.BaniraKeySpec;
import xin.vanilla.banira.internal.forge.client.ForgeKeyBindingService;

import javax.annotation.Nonnull;

/**
 * 旧包名下的按键注册别名；新代码优先使用 {@link BaniraInput}。
 */
@OnlyIn(Dist.CLIENT)
public final class BaniraKeyBindings {

    private BaniraKeyBindings() {
    }

    @Nonnull
    public static String defaultCategory(@Nonnull String modId) {
        return BaniraInput.defaultCategory(modId);
    }

    @Nonnull
    public static String descriptionId(@Nonnull String modId, @Nonnull String suffix) {
        return BaniraInput.descriptionId(modId, suffix);
    }

    @Nonnull
    public static BaniraKeyHandle register(@Nonnull String modId, @Nonnull String suffix, int defaultKeyScanCode) {
        return BaniraInput.registerKey(modId, suffix, defaultKeyScanCode);
    }

    @Nonnull
    public static BaniraKeyHandle register(@Nonnull String modId, @Nonnull String suffix, int defaultKeyScanCode, @Nonnull String categoryTranslationKey) {
        return BaniraInput.registerKey(modId, suffix, defaultKeyScanCode, categoryTranslationKey);
    }

    @Nonnull
    public static BaniraKeySpec spec(@Nonnull String modId, @Nonnull String suffix) {
        return BaniraInput.spec(modId, suffix);
    }

    public static void flushPendingRegistrations() {
        ForgeKeyBindingService.flushPendingRegistrations();
    }
}
