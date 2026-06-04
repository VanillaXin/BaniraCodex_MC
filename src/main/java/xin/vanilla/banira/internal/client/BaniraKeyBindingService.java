package xin.vanilla.banira.internal.client;

import net.minecraft.client.settings.KeyBinding;
import xin.vanilla.banira.client.util.BaniraKeyHandle;

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

    public static BaniraKeyHandle create(@Nonnull String descriptionId, int defaultKey, @Nonnull String categoryId) {
        KeyBinding binding = new KeyBinding(descriptionId, defaultKey, categoryId);
        return new BaniraKeyHandle(descriptionId, categoryId, defaultKey, binding);
    }

    public static void register(@Nonnull BaniraKeyHandle handle) {
        KeyBinding binding = handle.nativeBinding(KeyBinding.class);
        if (binding == null) {
            throw new IllegalArgumentException("Unsupported key binding handle: " + handle);
        }
        register(binding);
    }

    public static boolean isDown(@Nonnull BaniraKeyHandle handle) {
        KeyBinding binding = handle.nativeBinding(KeyBinding.class);
        return binding != null && binding.isDown();
    }

    public static boolean consumeClick(@Nonnull BaniraKeyHandle handle) {
        KeyBinding binding = handle.nativeBinding(KeyBinding.class);
        return binding != null && binding.consumeClick();
    }
}
