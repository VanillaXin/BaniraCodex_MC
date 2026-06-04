package xin.vanilla.banira.client.util;

import javax.annotation.Nonnull;

/**
 * Stable client input queries for dependent mods.
 */
public final class BaniraInput {
    private BaniraInput() {
    }

    public static boolean isDown(@Nonnull BaniraKeyHandle key) {
        return key.isDown();
    }

    public static boolean consumeClick(@Nonnull BaniraKeyHandle key) {
        return key.consumeClick();
    }
}
