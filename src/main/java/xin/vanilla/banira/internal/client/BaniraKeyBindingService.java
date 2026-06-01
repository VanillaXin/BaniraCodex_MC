package xin.vanilla.banira.internal.client;

import net.minecraft.client.settings.KeyBinding;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Loader adapter hook for registering Minecraft key bindings.
 */
public final class BaniraKeyBindingService {
    private static Consumer<KeyBinding> registrar = binding -> {
        throw new IllegalStateException("No Banira key binding registrar has been installed");
    };

    private BaniraKeyBindingService() {
    }

    public static void installRegistrar(@Nonnull Consumer<KeyBinding> value) {
        registrar = Objects.requireNonNull(value, "value");
    }

    public static void register(@Nonnull KeyBinding binding) {
        registrar.accept(Objects.requireNonNull(binding, "binding"));
    }
}
