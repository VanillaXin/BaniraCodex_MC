package xin.vanilla.banira.internal.client;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Loader adapter hook for applying client-side mod logo overrides.
 */
public final class BaniraLogoPatchService {
    private static Consumer<Function<String, Optional<String>>> applier = resolver -> {
    };

    private BaniraLogoPatchService() {
    }

    public static void installApplier(@Nonnull Consumer<Function<String, Optional<String>>> value) {
        applier = Objects.requireNonNull(value, "value");
    }

    public static void apply(@Nonnull Function<String, Optional<String>> resolver) {
        applier.accept(Objects.requireNonNull(resolver, "resolver"));
    }
}
