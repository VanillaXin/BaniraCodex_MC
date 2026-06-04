package xin.vanilla.banira.client.util;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.internal.client.BaniraKeyBindingService;

/**
 * Loader-neutral handle for a registered client key binding.
 */
@Getter
@Accessors(fluent = true)
public final class BaniraKeyHandle {
    private final String descriptionId;
    private final String categoryId;
    private final int defaultKey;
    private final Object nativeBinding;

    public BaniraKeyHandle(String descriptionId, String categoryId, int defaultKey, Object nativeBinding) {
        this.descriptionId = descriptionId;
        this.categoryId = categoryId;
        this.defaultKey = defaultKey;
        this.nativeBinding = nativeBinding;
    }

    public boolean isDown() {
        return BaniraKeyBindingService.isDown(this);
    }

    public boolean consumeClick() {
        return BaniraKeyBindingService.consumeClick(this);
    }

    /**
     * Advanced escape hatch for branch-local integrations.
     */
    public <T> T nativeBinding(Class<T> type) {
        return type.isInstance(nativeBinding) ? type.cast(nativeBinding) : null;
    }
}
