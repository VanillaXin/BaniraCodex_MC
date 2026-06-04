package xin.vanilla.banira.api.client;

import xin.vanilla.banira.internal.forge.client.ForgeKeyBindingService;

import javax.annotation.Nonnull;

/**
 * 客户端输入 API 入口；各分支只需要保证同名方法语义一致。
 */
public final class BaniraInput {

    private BaniraInput() {
    }

    @Nonnull
    public static String defaultCategory(@Nonnull String modId) {
        requireModId(modId);
        return String.format("key.%s.categories", modId);
    }

    @Nonnull
    public static String descriptionId(@Nonnull String modId, @Nonnull String suffix) {
        requireModId(modId);
        requireSuffix(suffix);
        return String.format("key.%s.%s", modId, suffix);
    }

    @Nonnull
    public static BaniraKeyHandle registerKey(@Nonnull String modId, @Nonnull String suffix, int defaultKey) {
        return registerKey(spec(modId, suffix).defaultKey(defaultKey));
    }

    @Nonnull
    public static BaniraKeyHandle registerKey(@Nonnull String modId, @Nonnull String suffix, int defaultKey, @Nonnull String category) {
        return registerKey(spec(modId, suffix).defaultKey(defaultKey).category(category));
    }

    @Nonnull
    public static BaniraKeyHandle registerKey(@Nonnull BaniraKeySpec spec) {
        validate(spec);
        return ForgeKeyBindingService.register(spec);
    }

    @Nonnull
    public static BaniraKeySpec spec(@Nonnull String modId, @Nonnull String suffix) {
        requireModId(modId);
        requireSuffix(suffix);
        return new BaniraKeySpec().modId(modId).suffix(suffix);
    }

    private static void validate(@Nonnull BaniraKeySpec spec) {
        requireModId(spec.modId());
        requireSuffix(spec.suffix());
    }

    private static void requireModId(@Nonnull String modId) {
        if (modId.isEmpty()) {
            throw new IllegalArgumentException("modId must be non-empty");
        }
    }

    private static void requireSuffix(@Nonnull String suffix) {
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must be non-empty");
        }
    }
}
