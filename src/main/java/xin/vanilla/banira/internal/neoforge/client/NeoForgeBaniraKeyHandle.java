package xin.vanilla.banira.internal.neoforge.client;

import net.minecraft.client.KeyMapping;
import xin.vanilla.banira.api.client.BaniraKeyHandle;

import javax.annotation.Nonnull;

/**
 * NeoForge 1.21.1 的按键句柄适配，公共 API 不暴露 KeyMapping。
 */
final class NeoForgeBaniraKeyHandle implements BaniraKeyHandle {
    private final KeyMapping binding;
    private final String category;
    private final int defaultKey;

    NeoForgeBaniraKeyHandle(@Nonnull KeyMapping binding, @Nonnull String category, int defaultKey) {
        this.binding = binding;
        this.category = category;
        this.defaultKey = defaultKey;
    }

    @Nonnull
    @Override
    public String descriptionId() {
        return binding.getName();
    }

    @Nonnull
    @Override
    public String category() {
        return category;
    }

    @Override
    public int defaultKey() {
        return defaultKey;
    }

    @Override
    public int currentKey() {
        return binding.getKey().getValue();
    }

    @Override
    public boolean isDown() {
        return binding.isDown();
    }

    @Override
    public boolean consumeClick() {
        return binding.consumeClick();
    }

    KeyMapping binding() {
        return binding;
    }
}
