package xin.vanilla.banira.internal.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 将现有 {@link net.minecraft.network.FriendlyByteBuf} 读写方法适配为 NeoForge {@link StreamCodec}
 */
public final class BaniraStreamCodecs {
    private BaniraStreamCodecs() {
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> registryBuf(
            BiConsumer<T, RegistryFriendlyByteBuf> encoder,
            Function<RegistryFriendlyByteBuf, T> decoder) {
        return new StreamCodec<>() {
            @Nonnull
            @Override
            public T decode(@Nonnull RegistryFriendlyByteBuf buf) {
                return decoder.apply(buf);
            }

            @Override
            public void encode(@Nonnull RegistryFriendlyByteBuf buf, @Nonnull T value) {
                encoder.accept(value, buf);
            }
        };
    }
}
