package xin.vanilla.banira.internal.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import xin.vanilla.banira.client.util.BaniraKeyHandle;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Loader adapter hook for registering Minecraft key bindings.
 */
public final class BaniraKeyBindingService {
    private static Consumer<KeyMapping> registrar = binding -> {
        throw new IllegalStateException("No Banira key binding registrar has been installed");
    };

    private BaniraKeyBindingService() {
    }

    public static void installRegistrar(@Nonnull Consumer<KeyMapping> value) {
        registrar = Objects.requireNonNull(value, "value");
    }

    public static void register(@Nonnull KeyMapping binding) {
        registrar.accept(Objects.requireNonNull(binding, "binding"));
    }

    public static BaniraKeyHandle create(@Nonnull String descriptionId, int defaultKey, @Nonnull String categoryId) {
        KeyMapping binding = new KeyMapping(descriptionId, defaultKey, categoryId);
        return new BaniraKeyHandle(descriptionId, categoryId, defaultKey, binding);
    }

    public static void register(@Nonnull BaniraKeyHandle handle) {
        KeyMapping binding = handle.nativeBinding(KeyMapping.class);
        if (binding == null) {
            throw new IllegalArgumentException("Unsupported key binding handle: " + handle);
        }
        register(binding);
    }

    public static boolean isDown(@Nonnull BaniraKeyHandle handle) {
        KeyMapping binding = handle.nativeBinding(KeyMapping.class);
        return binding != null && binding.isDown();
    }

    public static int currentKey(@Nonnull BaniraKeyHandle handle) {
        KeyMapping binding = handle.nativeBinding(KeyMapping.class);
        return binding != null ? InputConstants.getKey(binding.saveString()).getValue() : handle.defaultKey();
    }

    public static boolean consumeClick(@Nonnull BaniraKeyHandle handle) {
        KeyMapping binding = handle.nativeBinding(KeyMapping.class);
        return binding != null && binding.consumeClick();
    }
}
